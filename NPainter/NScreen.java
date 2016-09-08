package NPainter;

import Painter.Palette.Palette;
import Painter.Screen;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by ainozemtsev on 17.06.16.
 */
public class NScreen extends Screen {



    public enum Table {INK, PAPER;}
    @Override
    protected Palette createPalette() {
        return new NPalette();
    }

    @Override
    final protected void setGridFactor() {
        GRID_FACTOR_Y = 8;
        GRID_FACTOR_X = 8;
    }

    @Override
    public Table mapColorTable(int table) {
        return Table.values()[table];
    }

    @Override
    public void importSCR(InputStream stream) throws IOException {
        byte[] pix = new byte[2048 * 3];
        byte[] attr = new byte[768];
        stream.read(pix);
        stream.read(attr);
        ByteArrayOutputStream as = new ByteArrayOutputStream(256 * 192 + 32 * 24);
        boolean bright;
        for (int x = 0; x < 256; x++) {
            byte[] buf = new byte[192];
            for (int y = 0; y < 192; y++) {
                byte a = attr[(y & 0xf8) * 4 + x / 8];
                bright = (a & 0x40) != 0;
                int b = pix[(x >> 3) + 256 * (y & 7) + 4 * (y & 0x38) + 32 * (y & 0xc0)] >> (7 - (x & 7));
                byte p = (byte) ((b & 1) * 2 + (bright ? 0 : 1));
                if ((a & 7) == 0 && p == 3) p = 2;
                if ((a & 0x38) == 0 && p == 1) p = 0;
                buf[y] = p;
            }
            as.write(buf);
        }
        for (int x = 0; x < 32; x++) {
            byte[] buf = new byte[24];
            for (int y = 0; y < 24; y++) {
                byte a = attr[x + 32 * y];
                buf[y] = (byte) (a & 0x3f);
            }
            as.write(buf);
        }
        image.load(new ByteArrayInputStream(as.toByteArray()));

        int[] ink = {0x00, 0x14, 0x18, 0x1e, 0x1c, 0x1a, 0x16, 0x12};
        for (int i = 1; i < 8; i++) ink[i] += (ink[i] + 0x10) << 6;
        palette.setPalette(ink, ink);

    }

    public enum Mode {
        Color4("4 colors mode"),
        Color5("5+1 colors mode"),
        Color6("6 colors mode"),
        Color8("8 colors mode");

        String name;
        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public NScreen() {
        super();
        mode = Mode.Color6;
    }

    public static int paperFromAttr(byte attr) { return (attr >> 3) & 7; }

    public static int inkFromAttr(byte attr) { return attr & 7; }

    private static int fromAttr(byte attr, Table table) {
        return (table == Table.INK) ? inkFromAttr(attr) : paperFromAttr(attr);
    }

    public static byte packAttr(int ink, int paper) {
        byte b = 0;
        return  inkToAttr(paperToAttr(b,paper),ink);
    }

    private static byte paperToAttr(byte attr, int paper) { return (byte) ((attr & 7) | (paper<<3));}

    private static byte inkToAttr(byte attr, int ink) { return (byte) ((attr & 0x38) | ink);}

    private static byte toAttr(byte attr, int value, Table table) {
        return (table == Table.INK) ? inkToAttr(attr,value) : paperToAttr(attr,value);
    }

    private Color getInkRBGColor(int index, int shift) {
        return palette.getRGBColor(Table.INK, index, shift);
    }

    private Color getPaperRGBColor(int index, int shift) {
        return palette.getRGBColor(Table.PAPER, index, shift);
    }

    @Override
    public Color getPixelColor(int x, int y) {
        int xx = x & 0xfffe;
        byte attr = image.getAttr(x, y);
        int pix1 = image.getPixel(xx, y);
        int pix2 = image.getPixel(xx+1, y);
        int pix = (x==xx) ? pix1 : pix2;

        if (mode == Mode.Color5) {
            if ((pix1 ^ pix2) == 1 && (pix1 & pix2)>1)
                return getInkRBGColor(paperFromAttr(attr), pix2 & 1);
            if ((pix1==1 || pix2==1) && (pix1>1 || pix2>1)) {
                if (pix > 1) return getInkRBGColor(paperFromAttr(attr), pix & 1);
                else return getPaperRGBColor(paperFromAttr(attr), 0);
            }
        }

        if ((pix1 ^ pix2) == 2 && (pix1 & pix2) == 1 && mode != Mode.Color4 )
                return getInkRBGColor(paperFromAttr(attr), (pix1 & 2) == 2 ? 0 : 1);
        if ((pix1 ^ pix2) == 3 && (pix1 * pix2) != 0 && mode == Mode.Color8 )
                return getPaperRGBColor(inkFromAttr(attr), (pix1 & 2) == 2 ? 0 : 1);

        if (pix < 2)
            return getPaperRGBColor(paperFromAttr(attr), pix & 1);
        else
            return getInkRBGColor(inkFromAttr(attr), pix & 1);
    }

    @Override
    public Status getStatus(int x, int y) {
        if (enhancedColors[0] ==-1 && enhancedColors[1] == -1) return Status.Normal;
        byte attr = image.getAttr(x,y);
        int ink = inkFromAttr(attr);
        int paper = paperFromAttr(attr);
        int pix = image.getPixel(x,y);
        if (pix<2)
            return (paper == enhancedColors[Table.PAPER.ordinal()]) ? Status.Enhanced : Status.Dimmed;
        else
            return (ink == enhancedColors[Table.INK.ordinal()]) ? Status.Enhanced : Status.Dimmed;
    }

    @Override
    public Pixel getPixelDescriptor(int x, int y) {
        int v = image.getPixel(x, y);
        byte attr = image.getAttr(x, y);
        return new Pixel((v<2)? Table.PAPER: Table.INK,
                (v < 2) ? paperFromAttr(attr): inkFromAttr(attr),
                v & 1);
    }

    @Override
    protected byte attrFromDesc(Pixel pixel, byte oldAttr) {
        if (pixel.table == Table.INK) {
            return inkToAttr(oldAttr, pixel.index);
        } else {
            return paperToAttr(oldAttr, pixel.index);
        }
    }

    @Override
    protected byte pixelFromDesc(Pixel pixel, byte oldPixel, int x, int y) {
        byte s;
        if (pixel.table == Table.INK) s = 2;
        else s = 0;
        s = (byte) (pixel.shift | s);
        return s;
    }

    @Override
    public void rearrangeColorTable(int t, int[] order) {
        Table table = mapColorTable(t);
        image.forEachAttr( (x, y, attr) -> {
            int a = fromAttr(attr, table);
            return toAttr(attr, order[a], table);
        });
        palette.reorder(table, order);
        fireImageChanged();
    }

    @Override
    public Map<String, Dimension> getResolutions() {
        HashMap<String, Dimension> m = new HashMap<>();
        m.put("256x192", new Dimension(256,192));
        m.put("320x200", new Dimension(320,200));
        m.put("320x240", new Dimension(320,240));
        return m;
    }

    @Override
    protected FileNameExtensionFilter getFileNameExtensionFilter() {
        return new FileNameExtensionFilter("New screen", "scrn");
    }

    @Override
    public  Map<String, Consumer<Integer[]>> getSpecialMethods() {
        Map<String, Consumer<Integer[]>> m = new HashMap<>();
        final int ink = Table.INK.ordinal();
        final int paper = Table.PAPER.ordinal();
        m.put("Flip ink",   (i) -> flipColorCell(Table.INK,     i[ink]));
        m.put("Flip paper", (i) -> flipColorCell(Table.PAPER,   i[paper]));
        m.put("Flip all inks", (dummy) -> {
            for (int i =0; i < palette.getColorsCount(Table.INK); i++) flipColorCell(Table.INK, i);
        });
        m.put("Flip all papers",(dummy) -> {
            for (int i =0; i < palette.getColorsCount(Table.PAPER); i++) flipColorCell(Table.PAPER, i);
        });
        m.put("Inverse palette", (dummy) -> inverseColors());
        m.put("Swap ink0 <-> paper0", (c) -> swapInkPaper(c[ink], c[paper], 0));
        m.put("Swap ink1 <-> paper1", (c) -> swapInkPaper(c[ink], c[paper], 1));
        return m;
    }

    private void flipColorCell(Table table, int index){
        image.forEachPixel( (x, y, b, a) -> {
            if (table == Table.INK)
                return (byte) (inkFromAttr(a) == index && b >= 2 ? 5 - b : b);
            else
                return (byte) (paperFromAttr(a) == index && b < 2 ? 1 - b : b);
        });
        int c = palette.getColorCell(table, index);
        palette.setColorCell(Palette.combine(Palette.second(c),Palette.first(c)), table,index);
    }

    private void inverseColors() {
        image.forEachPixel((x, y, b, a) -> (byte) (b ^ 2));
        image.forEachAttr((x, y, b) -> (byte) (((b & 7) << 3) | ((b >> 3) & 7)));
        int l = Integer.min(palette.getColorsCount(Table.INK), palette.getColorsCount(Table.PAPER));
        for (int i = 0; i < l; i++) {
            int ink = palette.getColorCell(Table.INK,i);
            int paper = palette.getColorCell(Table.PAPER,i);
            palette.setColorCell(ink, Table.PAPER,i);
            palette.setColorCell(paper, Table.INK, i);
        }
    }

    private void swapInkPaper(int ink, int paper, int shift) {
        beginDraw();
        image.forEachPixel((x, y, b, a) -> {
            final boolean f = (inkFromAttr(a) == ink && paperFromAttr(a) == paper && (b&1) == shift);
            if (f) undo.add(x, y, b, a, (byte) (b ^ 2), a);
            return (byte) (f ? b ^ 2 : b);
        });
        int i = palette.getColorCell(Table.INK, ink);
        int i1 = Palette.split(i, shift);
        int p = palette.getColorCell(Table.PAPER, paper);
        int p1 = Palette.split(p, shift);
        palette.setColorCell(Palette.replace(i, p1, shift), Table.INK, ink);
        palette.setColorCell(Palette.replace(p, i1, shift), Table.PAPER, paper);
        endDraw();
    }

}
