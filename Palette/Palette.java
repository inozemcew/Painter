package Painter.Palette;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by ainozemtsev on 18.11.15.
 * Palette tables
 */
public class Palette {

    public enum Table {INK, PAPER}

    public static final int COLORS_PER_CELL = 2;

    private int[][] colorTable = new int[2][8];

    public static class PixelDescriptor {
        public final Palette.Table table;
        public final int index;
        public final int shift;

        public PixelDescriptor(Palette.Table table, int index, int shift) {
            this.index = index;
            this.shift = shift;
            this.table = table;
        }
    }


    public interface PaletteChangeListener {
        void paletteChanged();
    }

    private static class PaletteChangeListenerItem {
        private Table table;
        private int index;
        private PaletteChangeListener listener;

        public PaletteChangeListenerItem(Table table, int index, PaletteChangeListener listener) {
            this.table = table;
            this.index = index;
            this.listener = listener;
        }
    }

    private ArrayList<PaletteChangeListenerItem> listeners = new ArrayList<>();
    private boolean locked = false;

    {
        for (int i = 0; i<8; i++) {
            colorTable [1][i] = (i+16) | ((i+48)<<6);
            colorTable [0][i] = (i+40) | ((i+56)<<6);
        }
    }


    public static int combine(int... f)     {
        int result = 0;
        for (int i=0; i<f.length;i++)
            result |= f[i]<<(6*i);
        return  result; }
    public static int split  (int c, int shift) { return (c>>(6*shift)&63); }
    public static int first  (int c)            { return split(c,0); }
    public static int second (int c)            { return split(c,1); }
    public static int replace (int cc, int c, int shift) {
        return (cc & (~(63<<(6*shift)))) | (c << (6 * shift));
    }

    public int getColorCell(Table table, int index) {
        return colorTable[table.ordinal()][index];
    }

    public void setColorCell(int value, Table table, int index) {
        colorTable[table.ordinal()][index] = value;
        fireChangeEvent(table,index);
    }

    public void addChangeListener(PaletteChangeListener listener, Table table, int index) {
        this.listeners.add(new PaletteChangeListenerItem(table,index,listener));
    }

    public void addChangeListener(PaletteChangeListener listener) {
        this.listeners.add(new PaletteChangeListenerItem(null,-1,listener));
    }

    protected void fireChangeEvent(Table table, int index) {
        for (PaletteChangeListenerItem i : this.listeners) {
            if (i.table == null) {
                if (!locked) i.listener.paletteChanged();
            } else {
                if (i.table == table && i.index == index) i.listener.paletteChanged();
            }
        }
    }

    private void setLocked(boolean locked) {
        if (this.locked && !locked)
            listeners.stream().filter(i -> i.table == null).forEach(i -> i.listener.paletteChanged());
        this.locked = locked;
    }

    public void loadPalette(DataInputStream stream) throws IOException {
        for (int i = 0; i < 8; i++) setColorCell(stream.readInt(), Table.INK,i);
        for (int i = 0; i < 8; i++) setColorCell(stream.readInt(), Table.PAPER,i);
    }

    public void savePalette(DataOutputStream stream) throws IOException {
        for (int i = 0; i < 8; i++) stream.writeInt(getColorCell(Table.INK,i));
        for (int i = 0; i < 8; i++) stream.writeInt(getColorCell(Table.PAPER,i));
    }

    public void setPalette(int[] ink, int[] paper) {
        setLocked(true);
        for (int i=0; i<8; i++){
            setColorCell(ink[i],Table.INK,i);
            setColorCell(paper[i],Table.PAPER,i);
        }
        setLocked(false);
    }

    public int[] getPalette(Table table) {
        return colorTable[table.ordinal()];
    }


    public Color getRGBColor(Palette.Table table, int index, int fs) {
        return toRGB(split(getColorCell(table, index),fs));
    }

    public Color getInkRBGColor(int index, int shift) {
        return getRGBColor(Table.INK, index, shift);
    }

    public Color getPaperRGBColor(int index, int shift) {
        return getRGBColor(Table.PAPER,index, shift);
    }

    static Color toRGB2(int index) {
        float x=1,y=0,z=0,r,g,b;
        if ((index & 1) != 0) y = 0.5f;
        if ((index & 2) != 0) { x=1-x; y=1-y; z=1-z; }
        switch (index & 12) {
            case 0: { r=y; g=y; b=y; break;}
            case 4: { r=y; g=z; b=x; break;}
            case 8: { r=x; g=y; b=z; break;}
            default:{ r=z; g=x; b=y; break;}
        }
        float l = (index & 48)/96.0f+0.5f;
        return new Color(r*l, g*l, b*l);
    }
    public static Color toRGB1(int index) {
        float x=1,y=0,z=0,r,g,b;
        if ((index & 1) != 0) y = 0.5f;
        if ((index & 2) != 0) { x=1-x; y=1-y; z=1-z; }
        switch (index & 12) {
            case 0: { r=z; g=y; b=y; break;}
            case 4: { r=y; g=z; b=x; break;}
            case 8: { r=x; g=y; b=z; break;}
            default:{ r=z; g=x; b=y; break;}
        }
        //float l = (index & 48)/96.0f+0.5f;
        //return new Color(r*l, g*l, b*l);
        //float l = 0.75f - ( (index & 32) == 0  ? 0.375f: 0f);
        //float m =  (index & 16) == 0  ? 0 : 0.25f;
        //float l = 0.8125f - ( (index & 32) == 0  ? 0.375f: 0f);
        //float m =  (index & 16) == 0  ? 0 : 0.1875f;
        float l = 0.8f - ( (index & 32) == 0  ? 0.33f: 0f);
        float m =  (index & 16) == 0  ? 0 : 0.25f;
        //return new Color(r*l+m,g*l+m,b*l+m);
        return new Color(l*(r+m),l*(g+m),l*(b+m));
    }

    private static Color[] colorCache = new Color[64];

    static {
        for (int index = 0; index<64; index++) {
            int x=2,y=0,z=0,r,g,b;
            if ((index & 1) != 0) y = 1;
            if ((index & 2) != 0) { x=2-x; y=2-y; z=2-z; }
            switch (index & 12) {
                case 0: { r=z; g=y; b=y; break;}
                case 4: { r=y; g=z; b=x; break;}
                case 8: { r=x; g=y; b=z; break;}
                default:{ r=z; g=x; b=y; break;}
            }
            int l =  (index & 48) == 0  ? 64: 80;
            int m =  (index & 48)*2;
            m = m>95 ? 95:m;
            //return new Color(r*l+m,g*l+m,b*l+m);
            colorCache[index] = new Color(l*r+m,l*g+m,l*b+m);
        }
    }

    public static Color toRGB(int index) {
        return colorCache[index & 63];
    }

    /*
    4 128   0   0| 5 128  64   0| 6   0 128 128| 7   0  64 128| 8   0 128   0| 9   0 128  64|10 128   0 128|11 128   0  64|12   0   0 128|13  64   0 128|14 128 128   0|15  64 128   0
   20 170   0   0|21 170  85   0|22   0 170 170|23   0  85 170|24   0 170   0|25   0 170  85|26 170   0 170|27 170   0  85|28   0   0 170|29  85   0 170|30 170 170   0|31  85 170   0
   36 213   0   0|37 213 106   0|38   0 213 213|39   0 106 213|40   0 213   0|41   0 213 106|42 213   0 213|43 213   0 106|44   0   0 213|45 106   0 213|46 213 213   0|47 106 213   0
   52 255   0   0|53 255 128   0|54   0 255 255|55   0 128 255|56   0 255   0|57   0 255 128|58 255   0 255|59 255   0 128|60   0   0 255|61 128   0 255|62 255 255   0|63 128 255   0
*/


    public static Converter createConverter() {
        return new Converter();
    }

    public static int fromRGB(Color color,int[] indices){
        int bestIndex = 0;
        double bestYUV = Integer.MAX_VALUE;
        for (int i = 0; i < indices.length; i++){
            final double yuv = getYuvDiff(color, indices[i]);
            if (yuv < bestYUV) {
                bestYUV = yuv;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int[] allColorIndices = IntStream.range(0,64).toArray();

    public static int fromRGB(Color color) {
        return fromRGB(color,allColorIndices);
    }

    private static double getYuvDiff(Color color, int i) {
        final int dr = color.getRed() - toRGB(i).getRed();
        final int dg = color.getGreen() - toRGB(i).getGreen();
        final int db = color.getBlue() - toRGB(i).getBlue();
        final double y = 0.299 * dr + 0.587 * dg + 0.114 * db;
        final double u = -0.14713 * dr - 0.28886 * dg + 0.436 *db;
        final double v = 0.615 * dr  - 0.51499 * dg - 0.10001 * db;
        //final int d = dr * dr + dg * dg + db * db;
        return (y*y)/10 +  u*u + v*v;
    }

    public static int fromRGB1(Color color) {
        int b=color.getBlue(), r = color.getRed(), g = color.getGreen();
        int l = Integer.max(g,Integer.max(r,b));
        if (Integer.max(Math.abs(g-r), Math.abs(g-b))<16) {
            final Integer m = (r+g+b)/3;
            int[][] t = {{0,0},{32,16},{64,32},{96,48},{144,2},{192,18},{224,34},{255,50}};
            return Stream.of(t).filter(x -> m <= x[0]+16).mapToInt(x->x[1]).findFirst().orElse(50);
        }


        final int m = (l<160) ? 0 : ( (l<208) ? 16 : ( (l<240) ? 32 : 48) );
        if (l == b) {
            if (b-r < 16) return m + 14;
            if (b-g < 16) return m + 10;
            if (Math.abs(r-g)>l/8) return (r<g) ? m + 11 : m + 5;
            return m + 4;
        }
        if (l == r){
            if (r-g <32) return m + 6;
            if (r-b <20) return m + 14;
            if (b>r/2 && g> r/2) return m+3;
            if (Math.abs(b-g)>l/8) return (b<g) ? m + 9 : m + 15;
            return m + 8;
        }
        if (Math.abs(r-b)<16) return m+12;
        return (r<b) ? m + 13 : m + 7;
    }

    public static void main (String[] argv) {
        for (int i = 0; i < 64; i++) {
            final Color rgb = toRGB(i);
            System.out.printf("%2d %3d %3d %3d %2d|", i, rgb.getBlue(), rgb.getRed(), rgb.getGreen(),fromRGB(rgb));
            if (i % 8 == 7) System.out.println();
            if (i % 16 == 15) System.out.println();
        }
    }
}

class Converter implements ColorConverting {
    HashMap<Color,Integer> cache = new HashMap<>(16);
    public int fromRGB(Color color) {
        return cache.computeIfAbsent(color, Palette::fromRGB);
    }
}
