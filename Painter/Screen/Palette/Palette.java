package Painter.Screen.Palette;

import Painter.Screen.UndoRedo;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static sun.misc.Version.println;

/**
 * Created by ainozemtsev on 18.11.15.
 * Palette tables
 */
public class Palette {

    private final int[][] colorTables;
    private final int[] cellsSizes;

    private final ArrayList<PaletteChangeListenerItem> listeners = new ArrayList<>();

    private static class PaletteChangeListenerItem {
        private final Integer table;
        private final int index;

        private final PaletteChangeListener listener;

        public PaletteChangeListenerItem(Enum<?> table, int index, PaletteChangeListener listener) {
            this(table == null ? null : table.ordinal(), index, listener);
        }

        public PaletteChangeListenerItem(Integer table, int index, PaletteChangeListener listener) {
            this.table = table;
            this.index = index;
            this.listener = listener;
        }
    }

    private boolean locked = false;

    public Palette(int tableCount, int[] tableSizes, int[] cellsSizes) {
        this.colorTables = new int[tableCount][];
        this.cellsSizes = new int[tableCount];
        for (int t = 0; t < tableCount; t++) {
            this.colorTables[t] = new int[tableSizes[t]];
            this.cellsSizes[t] = cellsSizes[t];
        }
    }


    public static int combine(int... f) {
        int result = 0;
        for (int i = 0; i < f.length; i++)
            result |= f[i] << (6 * i);
        return result;
    }

    public static int split(int c, int shift) {
        return (c >> (6 * shift) & 63);
    }

    public static int first(int c) {
        return split(c, 0);
    }

    public static int second(int c) {
        return split(c, 1);
    }

    public static int replace(int cc, int c, int shift) {
        return (cc & (~(63 << (6 * shift)))) | (c << (6 * shift));
    }


    public int getTablesCount() {
        return colorTables.length;
    }

    public int getColorsCount(Enum<?> table) {
        return getColorsCount(table.ordinal());
    }

    public int getColorsCount(int table) {
        return colorTables[table].length;
    }

    public int getCellSize(Enum<?> table) {
        return getCellSize(table.ordinal());
    }

    public int getCellSize(int table) {
        return cellsSizes[table];
    }

    public int getColorCell(Enum<?> table, int index) {
        return getColorCell(table.ordinal(), index);
    }

    public int getColorCell(int table, int index) {
        return colorTables[table][index];
    }

    public void setColorCell(int value, Enum<?> table, int index) {
        setColorCell(value, table.ordinal(), index);
    }

    public void setColorCell(int value, int table, int index) {
        int oldValue = getColorCell(table, index);
        colorTables[table][index] = value;
        if (undo != null) undo.add(new UndoColorItem(table, index, value, oldValue));
        fireChangeEvent(table, index);
    }

    private UndoRedo undo = null;

    private class UndoColorItem implements UndoRedo.Item {
        private final int table, index, newValue, oldValue;

        UndoColorItem(int table, int index, int newValue, int oldValue) {
            this.table = table;
            this.index = index;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        @Override
        public void undo() {
            setColorCell(this.oldValue, this.table, this.index);
        }

        @Override
        public void redo() {
            setColorCell(this.newValue, this.table, this.index);
        }
    }


    public void setUndo(UndoRedo undo) {
        this.undo = undo;
    }

    public void beginUpdate() {
        undo.start();
    }

    public void endUpdate() {
        undo.commit();
    }

    public void addChangeListener(PaletteChangeListener listener, int table, int index) {
        this.listeners.add(new PaletteChangeListenerItem(table, index, listener));
    }

    public void addChangeListener(PaletteChangeListener listener) {
        this.listeners.add(new PaletteChangeListenerItem((Enum<?>) null, -1, listener));
    }

    private void fireChangeEvent(Enum<?> table, int index) {
        fireChangeEvent(table.ordinal(), index);
    }

    private void fireChangeEvent(int table, int index) {
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

    private void unlockAndUpdateAll() {
        listeners.forEach(i -> i.listener.paletteChanged());
        this.locked = false;
    }

    public void loadPalette(DataInputStream stream) throws IOException {
        setLocked(true);
        try {
            for (int t = 0; t < colorTables.length; t++) {
                for (int i = 0; i < colorTables[t].length; i++)
                    setColorCell(stream.readInt(), t, i);
            }
        } finally {
            setLocked(false);
        }
    }

    public void loadPalette(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int[][] p = new int[colorTables.length][];
        for (int t = 0; t < colorTables.length; t++) {
            p[t] = (int[]) stream.readObject();
        }
        setPalette(p);
    }

    public void savePalette(DataOutputStream stream) throws IOException {
        for (int t = 0; t < colorTables.length; t++) {
            for (int i = 0; i < colorTables[t].length; i++)
                stream.writeInt(getColorCell(t, i));
        }
    }

    public void savePalette(ObjectOutputStream stream) throws IOException {
        for (int[] colorTable : colorTables) {
            stream.writeObject(colorTable);
        }
    }

    public void setPalette(int[]... palette) {
        setLocked(true);
        for (int t = 0; t < colorTables.length && t < palette.length; t++) {
            for (int i = 0; i < colorTables[t].length && t < palette[t].length; i++) {
                setColorCell(palette[t][i], t, i);
            }
        }
        setLocked(false);
    }

    public Color getRGBColor(Enum<?> table, int index, int fs) {
        return toRGB(split(getColorCell(table, index), fs));
    }

    public int[] findAttr(List<Integer> s) {

        class IndexIterator implements Iterator<int[]> {
            private final int[] counters;
            private final int[] ranges;
            private boolean counting = true;

            public IndexIterator(int[] ranges) {
                this.counters = new int[ranges.length];
                this.ranges = ranges;
            }

            @Override
            public boolean hasNext() {
                return counting;
            }

            @Override
            public int[] next() {
                int[] values = Arrays.copyOf(counters, counters.length);
                for (int j = 0; j < counters.length; j++) {
                    if (++counters[j] < ranges[j]) return values;
                    counters[j] = 0;
                }
                counting = false;
                return values;
            }
        }

        double bestDiff = Integer.MAX_VALUE;

        Iterator<int[]> iterator = new IndexIterator(Arrays.stream(colorTables).mapToInt(x -> x.length).toArray());
        int[] best = new int[colorTables.length];
        while (iterator.hasNext()) {
            int[] i = iterator.next();
            double n = 0;
            for (int k : s) {
                if (k != -2) {
                    n += IntStream.range(0, colorTables.length)
                            .flatMap(t -> {
                                        final int cell = getColorCell(t, i[t]);
                                        return IntStream.range(0, getCellSize(t))
                                                .map(m -> split(cell, m));
                                    })
                            .mapToDouble(c -> getColorDiff(k, c))
                            .min()
                            .orElse(Integer.MAX_VALUE);
                }
            }
            if (n == 0) return i;
            if (n < bestDiff) {
                bestDiff = n;
                best = i;
            }
        }

        return best;
    }

    public void reorder(Enum<?> table, int[] order) {
        reorder(table.ordinal(), order);
    }

    public void reorder(int table, int[] order) {
        final int[] p = colorTables[table];
        int[] cells = Arrays.copyOf(p, p.length);
        setLocked(true);
        for (int i = 0; i < cells.length; i++)
            setColorCell(cells[i], table, order[i]);
        setLocked(false);
    }

    static Color toRGB2(int index) {
        float x = 1, y = 0, z = 0, r, g, b;
        if ((index & 1) != 0) y = 0.5f;
        if ((index & 2) != 0) {
            x = 1 - x;
            y = 1 - y;
            z = 1 - z;
        }
        switch (index & 12) {
            case 0: {
                r = y;
                g = y;
                b = y;
                break;
            }
            case 4: {
                r = y;
                g = z;
                b = x;
                break;
            }
            case 8: {
                r = x;
                g = y;
                b = z;
                break;
            }
            default: {
                r = z;
                g = x;
                b = y;
                break;
            }
        }
        float l = (index & 48) / 96.0f + 0.5f;
        return new Color(r * l, g * l, b * l);
    }

    public static Color toRGB1(int index) {
        float x = 1, y = 0, z = 0, r, g, b;
        if ((index & 1) != 0) y = 0.5f;
        if ((index & 2) != 0) {
            x = 1 - x;
            y = 1 - y;
            z = 1 - z;
        }
        switch (index & 12) {
            case 0: {
                r = z;
                g = y;
                b = y;
                break;
            }
            case 4: {
                r = y;
                g = z;
                b = x;
                break;
            }
            case 8: {
                r = x;
                g = y;
                b = z;
                break;
            }
            default: {
                r = z;
                g = x;
                b = y;
                break;
            }
        }
        //float l = (index & 48)/96.0f+0.5f;
        //return new Color(r*l, g*l, b*l);
        //float l = 0.75f - ( (index & 32) == 0  ? 0.375f: 0f);
        //float m =  (index & 16) == 0  ? 0 : 0.25f;
        //float l = 0.8125f - ( (index & 32) == 0  ? 0.375f: 0f);
        //float m =  (index & 16) == 0  ? 0 : 0.1875f;
        float l = 0.8f - ((index & 32) == 0 ? 0.33f : 0f);
        float m = (index & 16) == 0 ? 0 : 0.25f;
        //return new Color(r*l+m,g*l+m,b*l+m);
        return new Color(l * (r + m), l * (g + m), l * (b + m));
    }

    private static final Color[] colorCache = new Color[64];
    private static final double[][] colorDiff = new double[64][];

    static {
        ColorSpace.MyPal.activate();
    }

    public void activateColorSpace(ColorSpace colorSpace) {
        setLocked(true);
        colorSpace.activate();
        unlockAndUpdateAll();
    }

    public enum ColorSpace {
        MyPal("64 colors") {
            @Override
            protected void activate() {
                for (int index = 0; index < 64; index++) {
                    int xh = 1, yh = 0, zh = 0;
                    int xl = 1, yl = 0, zl = 1;
                    int r, g, b;
                    yh = index & 1;
                    yl = 1 - yh;
                    if ((index & 2) != 0) {
                        xh = 1 - xh;
                        yh = yl ^ yh;
                        zh = 1 - zh;
                    }
                    switch (index & 12) {
                        case 0: {
                            r = zh * (1 + zl);
                            g = yh * (1 + yl);
                            b = yh * (1 + yl);
                            break;
                        }
                        case 4: {
                            r = yh * (1 + yl);
                            g = zh * (1 + zl);
                            b = xh * (1 + xl);
                            break;
                        }
                        case 8: {
                            r = xh * (1 + xl);
                            g = yh * (1 + yl);
                            b = zh * (1 + zl);
                            break;
                        }
                        default: {
                            r = zh * (1 + zl);
                            g = xh * (1 + xl);
                            b = yh * (1 + yl);
                            break;
                        }
                    }
                    int l = (index & 48) / 3 + 64; //(index & 32) == 0 ? 64 : 80;
                    int m = (index & 48) * 2;
                    m = m > 95 ? 95 : m;
                    colorCache[index] = new Color(l * r + m, l * g + m, l * b + m);
                }
                super.activate();
            }
        },

        Pure64("Pure 64 colors") {
            @Override
            protected void activate() {
                int[][] fc = {{0,0,0},{1,0,0},{0,1,0},{0,0,1}};
                int[][] hc = {{1,0,1},{0,1,0},{0,0,1},{1,0,0}};
                for (int j = 0; j < 64; j++ ) {
                    int l = 3 - (j >> 4);
                    int c = j & 0xf;
                    int b = c >> 2;
                    int i = ((c>>1) & 1);
                    int h = (c & 1);
                    int hb = hc[b][0] & h, hr = hc[b][1] & h, hg =hc[b][2] & h;
                    int fb = (fc[b][0]^i)|hb, fr = (fc[b][1]^i)|hr, fg =(fc[b][2]^i)|hg;

                    int bb = (255*fb  - l*42) / (hb+1);
                    bb = (bb<0)?0:bb;
                    int rr = (255*fr  -l*42) / (hr+1);
                    rr = (rr<0)?0:rr;
                    int gg = (255*fg  -l*42) / (hg+1);
                    gg = (gg<0)?0:gg;
                    if (c==0) {
                        rr= 96-l*32; gg=rr; bb=rr;
                    }
                    colorCache[j] = new Color(rr,gg,bb);
                }
                super.activate();
            }
        },

        Pure64of512 ("64 of 512 colors") {
            @Override
            protected void activate() {
                int[][] fc = {{0,0,0,1},{1,0,0,0},{0,1,0,0},{0,0,1,0}};
                int[] hc = {0,2,3,1};
                //int[][] hc = {{1,0,1},{0,1,0},{0,0,1},{1,0,0}};
                for (int j = 0; j < 64; j++ ) {
                    int luma = (j >> 4);
                    int c = j & 0xf;
                    int b = c >> 2;
                    int bh = hc[b];
                    int inv = ((c>>1) & 1);
                    int halfbr = (c & 1);
                    int hb = (fc[bh][0] | fc[bh][3]) & halfbr, hr = fc[bh][1] & halfbr, hg = (fc[bh][2]| fc[bh][3]) & halfbr;
                    int fb = (fc[b][0]^inv)|hb, fr = (fc[b][1]^inv)|hr, fg =(fc[b][2]^inv)|hg;

                    int k = 36;
                    int bb = (fb&~hb)*4 + luma*fb + hb*2;
                    int rr = (fr&~hr)*4 + luma*fr + hr*2;
                    int gg = (fg&~hg)*4 + luma*fg + hg*2;
                    if (c==0) {
                        bb=luma; rr=luma; gg=luma;
                    }
                    colorCache[j] = new Color(rr*k, gg*k, bb*k);
                }
                super.activate();
            }
        },

        Pal3x2("2bit per component") {
            @Override
            protected void activate() {
                int[] r = {0,0,1,2, 0,1,1,1, 1,2,0,0, 0,0,1,2,
                           0,0,2,3, 0,1,2,1, 2,3,0,0, 0,0,2,3,
                           0,0,3,3, 0,2,3,2, 3,3,0,0, 0,0,3,3,
                           1,0,3,3, 1,2,3,2, 3,3,1,1, 1,1,3,3};
                int[] g = {0,1,1,1, 0,0,1,2, 0,1,1,1, 1,2,0,0,
                           0,2,2,1, 0,0,2,3, 0,1,2,1, 2,3,0,0,
                           1,2,2,2, 0,0,3,3, 0,2,3,2, 3,3,0,0,
                           1,3,3,2, 1,1,3,3, 1,2,3,2, 3,3,1,1};
                int[] b = {0,1,1,1, 1,2,0,0, 0,0,1,2, 0,1,1,1,
                           0,2,2,2, 2,3,0,0, 0,0,2,3, 0,1,2,1,
                           1,2,3,2, 3,3,0,0, 0,0,3,3, 0,2,3,2,
                           1,3,3,3, 3,3,1,1, 1,1,3,3, 1,2,3,2};

                int[] l = {0, 128, 192, 255};

                for (int i = 0; i < 64; i++)
                    colorCache[i] = new Color(l[r[i]], l[g[i]], l[b[i]]);
                super.activate();
            }
        },

        Dark("Evenly spaced") {
            @Override
            protected void activate() {
                final int[][] fc = {{0,0,0},{1,0,0},{0,1,0},{0,0,1}};
                final int[][] hc = {{1,0,1},{0,1,0},{0,0,1},{1,0,0}};
                for (int j = 0; j < 64; j++ ) {
                    int l = (j >> 4);
                    int c = j & 0xf;
                    int b = c >> 2;
                    int i = ((c>>1) & 1);
                    int h = (c & 1);
                    int hb = hc[b][0] & h, hr = hc[b][1] & h, hg =hc[b][2] & h;
                    int fb = (fc[b][0]^i)|hb, fr = (fc[b][1]^i)|hr, fg =(fc[b][2]^i)|hg;

                    int k = 36;
                    int bb = (fb&~hb)*4 + l*fb;
                    int rr = (fr&~hr)*4 + l*fr;
                    int gg = (fg&~hg)*4 + l*fg;
                    if (c==0) {
                        bb=l; rr=l; gg=l;
                    }
                    int[] t = {0,63,95,127,63,127,191,255};
                    colorCache[j] = new Color(t[rr], t[gg], t[bb]);
                }
                super.activate();
            }
        },

        Pal64of128("64 of 256 colors") {
            @Override
            protected void activate() {
                int [] cs = {
                        0000,0202,0444,0242, 0400,0420,0044,0034, 0040,0042,0404,0402, 0004,0204,0440,0240,
                        0111,0313,0555,0353, 0500,0530,0155,0145, 0150,0153,0505,0503, 0105,0305,0550,0350,
                        0222,0424,0666,0464, 0600,0640,0066,0056, 0060,0064,0606,0604, 0006,0406,0660,0460,
                        0333,0535,0777,0575, 0700,0750,0177,0167, 0170,0175,0707,0705, 0107,0507,0770,0570
                };
                int[][] fc = {{0,0,0,1},{1,0,0,0},{0,1,0,0},{0,0,1,0}};
                int[] hc = {0,2,3,1};
                //int[][] hc = {{1,0,1},{0,1,0},{0,0,1},{1,0,0}};
                for (int j = 0; j < 64; j++ ) {
                    int luma = (j >> 4);
                    int c = j & 0xf;
                    int b = c >> 2;
                    int bh = hc[b];
                    int inv = ((c>>1) & 1);
                    int halfbr = (c & 1);
                    int hb = (fc[bh][0] | fc[bh][3]) & halfbr, hr = fc[bh][1] & halfbr, hg = (fc[bh][2]| fc[bh][3]) & halfbr;
                    int fb = (fc[b][0]^inv)|hb, fr = (fc[b][1]^inv)|hr, fg =(fc[b][2]^inv)|hg;

                    int k = 36;
                    int bb = (fb&~hb)*4 + ((luma*fb)|(luma&1)) + hb*2;
                    int rr = (fr&~hr)*4 + (luma*fr) + hr*2;
                    int gg = (fg&~hg)*4 + (luma*fg) + hg*2;
                    if (c==0) {
                        bb=luma; rr=luma; gg=luma;
                    }
                    gg = cs[j] % 8;
                    rr = cs[j] / 8 % 8;
                    bb = cs[j] / 64;

                    colorCache[j] = new Color(rr*k, gg*k, bb*k);
                }
                super.activate();
            }
        };

        private final String name;
        ColorSpace(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        protected void activate(){
            for (int i = 0; i < 64; i++) {
                colorDiff[i] = new double[64];
                for (int j = 0; j < 64; j++)
                    colorDiff[i][j] = calcYuvDiff(toRGB(i), j);
            }

        }
    }

    public static Color toRGB(int index) {
        return colorCache[index & 63];
    }

    public static double getColorDiff(int i, int j) {
        return colorDiff[i][j];
    }
    /*
    4 128   0   0| 5 128  64   0| 6   0 128 128| 7   0  64 128| 8   0 128   0| 9   0 128  64|10 128   0 128|11 128   0  64|12   0   0 128|13  64   0 128|14 128 128   0|15  64 128   0
   20 170   0   0|21 170  85   0|22   0 170 170|23   0  85 170|24   0 170   0|25   0 170  85|26 170   0 170|27 170   0  85|28   0   0 170|29  85   0 170|30 170 170   0|31  85 170   0
   36 213   0   0|37 213 106   0|38   0 213 213|39   0 106 213|40   0 213   0|41   0 213 106|42 213   0 213|43 213   0 106|44   0   0 213|45 106   0 213|46 213 213   0|47 106 213   0
   52 255   0   0|53 255 128   0|54   0 255 255|55   0 128 255|56   0 255   0|57   0 255 128|58 255   0 255|59 255   0 128|60   0   0 255|61 128   0 255|62 255 255   0|63 128 255   0
*/


    public static int fromRGB(Color color, int[] indices) {
        int bestIndex = 0;
        double bestYUV = Integer.MAX_VALUE;
        for (int i = 0; i < indices.length; i++) {
            final double yuv = calcYuvDiff(color, indices[i]);
            if (yuv < bestYUV) {
                bestYUV = yuv;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static final int[] allColorIndices = IntStream.range(0, 64).toArray();

    public static int fromRGB(Color color) {
        return fromRGB(color, allColorIndices);
    }

    private static double calcYuvDiff(Color color, int i) {
        final int dr = color.getRed() - toRGB(i).getRed();
        final int dg = color.getGreen() - toRGB(i).getGreen();
        final int db = color.getBlue() - toRGB(i).getBlue();
        final double y = 0.299 * dr + 0.587 * dg + 0.114 * db;
        final double u = -0.14713 * dr - 0.28886 * dg + 0.436 * db;
        final double v = 0.615 * dr - 0.51499 * dg - 0.10001 * db;
        //final int d = dr * dr + dg * dg + db * db;
        return (y * y) / 2 + u * u + v * v;
    }

    public static int fromRGB1(Color color) {
        int b = color.getBlue(), r = color.getRed(), g = color.getGreen();
        int l = Integer.max(g, Integer.max(r, b));
        if (Integer.max(Math.abs(g - r), Math.abs(g - b)) < 16) {
            final Integer m = (r + g + b) / 3;
            int[][] t = {{0, 0}, {32, 16}, {64, 32}, {96, 48}, {144, 2}, {192, 18}, {224, 34}, {255, 50}};
            return Stream.of(t).filter(x -> m <= x[0] + 16).mapToInt(x -> x[1]).findFirst().orElse(50);
        }


        final int m = (l < 160) ? 0 : ((l < 208) ? 16 : ((l < 240) ? 32 : 48));
        if (l == b) {
            if (b - r < 16) return m + 14;
            if (b - g < 16) return m + 10;
            if (Math.abs(r - g) > l / 8) return (r < g) ? m + 11 : m + 5;
            return m + 4;
        }
        if (l == r) {
            if (r - g < 32) return m + 6;
            if (r - b < 20) return m + 14;
            if (b > r / 2 && g > r / 2) return m + 3;
            if (Math.abs(b - g) > l / 8) return (b < g) ? m + 9 : m + 15;
            return m + 8;
        }
        if (Math.abs(r - b) < 16) return m + 12;
        return (r < b) ? m + 13 : m + 7;
    }

    public static void main(String[] argv) {
        ColorSpace.Pal64of128.activate();
        for (int i = 0; i < 64; i++) {
            if (i%16 == 0) System.out.println();
            System.out.printf("\t%02x %x%x%x",i,colorCache[i].getBlue()/36,colorCache[i].getRed()/36,colorCache[i].getGreen()/36);
            //final Color rgb = toRGB(i);
            //System.out.printf("%2d %3d %3d %3d %2d|", i, rgb.getBlue(), rgb.getRed(), rgb.getGreen(), fromRGB(rgb));
            //if (i % 8 == 7) System.out.println();
            //if (i % 16 == 15) System.out.println();
        }
        System.out.println();
        for (int i = 0; i < 64; i++) {
            if (i%16 == 0) System.out.println();
            final Color rgb = toRGB(i);
            System.out.printf("%2d %3d %3d %3d %2d|", i, rgb.getBlue(), rgb.getRed(), rgb.getGreen(), fromRGB(rgb));
            if (i % 8 == 7) System.out.println();
            if (i % 16 == 15) System.out.println();
        }
    }
}

