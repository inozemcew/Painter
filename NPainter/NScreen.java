package NPainter;

import Painter.Screen.ImageBuffer;
import Painter.Screen.Palette.Palette;
import Painter.Screen.Pixel;
import Painter.Screen.PixelProcessing;
import Painter.Screen.Screen;
import Painter.SpectrumScreen;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static NPainter.PixelProcessor.*;

/**
 * Created by ainozemtsev on 17.06.16.
 */
public class NScreen extends Screen {


    private final HashMap<String, Dimension> resolutions = new HashMap<>(); {
        resolutions.put("256x192", new Dimension(256, 192));
        resolutions.put("320x200", new Dimension(320, 200));
        resolutions.put("320x240", new Dimension(320, 240));
    }

    public enum Table {INK, PAPER}

    @Override
    final protected void setFactors() {
        GRID_FACTOR.setSize(8, 8);
        pixelFactor.setSize(2, 1);
        attrFactor.setSize(8, 8);
    }

    @Override
    public Table mapColorTable(int table) {
        return Table.values()[table];
    }

    @Override
    protected Palette createPalette() {
        return new NPalette();
    }

    @Override
    protected PixelProcessing createPixelProcessor() {
        return PixelProcessor.MODEX;
    }

    @Override
    public Status getStatus(Point pos) {
        if (enhancedColors[0] == -1 && enhancedColors[1] == -1) return Status.Normal;
        byte attr = getAttr(pos);
        byte pix = getPixelData(pos);
        Pixel pixel = pixelProcessor.unpackPixel(pix, attr, pos);
        return (pixel.index == enhancedColors[pixel.table.ordinal()]) ? Status.Enhanced : Status.Dimmed;
    }

    @Override
    public void rearrangeColorTable(int t, int[] order) {
        Table table = mapColorTable(t);
        beginDraw();
        image.forEachAttr((x, y, attr) -> {
            final int a = PixelProcessor.fromAttr(attr, table);
            final byte b = PixelProcessor.toAttr(attr, order[a], table);
            return b;
        });
        palette.reorder(table, order);
        endDraw();
        fireImageChanged();
    }


    @Override
    public Map<String, Dimension> getResolutions() {
        return resolutions;
    }

    @Override
    public FileNameExtensionFilter getFileNameExtensionFilter() {
        return new FileNameExtensionFilter("New screen", "scrn");
    }

    @Override
    public void importSCR(InputStream stream) throws IOException {
        SpectrumScreen spectrumScreen = new SpectrumScreen(stream);
        spectrumScreen.setOffset(128 - getImageWidth() / 2, 96 - getImageHeight() / 2);
        Point pos = new Point();
        for (int x = 0; x < getImageWidth(); x++) {
            for (int y = 0; y < getImageHeight(); y++) {
                SpectrumScreen.Pixel p = spectrumScreen.getPixel(x, y);
                Pixel pixel;
                if (p != null) {
                    if (p.value) {
                        pixel = new Pixel(Table.INK, p.ink, (p.ink > 0) ? p.bright : 0);
                    } else {
                        pixel = new Pixel(Table.PAPER, p.paper, (p.paper > 0) ? p.bright : 0);
                    }
                    pos.setLocation(x, y);
                    setPixel(pixel, pos);
                }
            }
        }
        setSpectrumPalette();
    }

    private void setSpectrumPalette() {
        int[] ink = {0x00, 0x24, 0x28, 0x2e, 0x2c, 0x2a, 0x26, 0x22};
        for (int i = 1; i < 8; i++) ink[i] += (ink[i] + 0x10) << 6;
        palette.setPalette(ink, ink);
    }

    private void setExtendedPalette() {
        int[] paper = {0x00, 0x24, 0x28, 0x2e, 0x2c, 0x2a, 0x26, 0x22};
        int[] ink = new int[8];
        ink[0] = 0x20 + ((paper[7] + 0x10) << 6);
        for (int i = 1; i < 8; i++) {
            ink[i] = paper[i] + 0x10;
            if ( i > 3 ) ink[i] +=  (ink[i-4]) << 6;
        }
        for (int i = 1; i < 4; i++) ink[i] +=  (ink[7-i]) << 6;
        palette.setPalette(ink, paper);
    }


    @Override
    public void load(InputStream stream, boolean old, boolean resize) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ImageBuffer img;
        Palette pal = createPalette();
        stream.mark(1024);
        ObjectInputStream iStream = new ObjectInputStream(stream);
        int x = 256, y = 192;
        if (old) {
            img = new ImageBuffer(256, 192, new Dimension(1, 1), new Dimension(8, 8));
            img.load(iStream);
            pal.loadPalette(iStream);

        } else {
            pal.loadPalette(iStream);
            x = iStream.readInt();
            if (x<255) {                    // old storage format was unpacked, one pixel per byte
                stream.reset();             // new storage format is packed, two pixels per byte
                super.load(stream,old,resize);         // so in new format the value of width is halved
                return;                     // and we don't need recoding if width less than 255
            }
            y = iStream.readInt();
            img = new ImageBuffer(x, y, new Dimension(1, 1), new Dimension(8, 8));
            img.load(iStream, x, y);
        }
        // do recoding using ByteArrayStream
        ObjectOutputStream os = new ObjectOutputStream(bs);
        pal.savePalette(os);
        os.writeInt(x / 2);
        os.writeInt(y);

        byte[] a = new byte[y];
        for (int i = 0; i < x; i += 2) {
            for (int j = 0; j < y; j++) {
                a[j] = (byte) (img.getPixel(i, j) | (img.getPixel(i + 1, j) << 4));
            }
            os.write(a);
        }
        a = new byte[y / 8];
        for (int i = 0; i < x; i += 8) {
            for (int j = 0; j < y; j += 8) {
                a[j / 8] = img.getAttr(i, j);
            }
            os.write(a);
        }
        os.close();

        super.load(new ByteArrayInputStream(bs.toByteArray()));
        stream.close();
    }

    SpecialMethods specialMethods = new SpecialMethods();

    @Override
    public Map<String, Consumer<Integer[]>> getSpecialMethods() {
        return specialMethods.getList();
    }

    private class SpecialMethods {

        private class Action implements Consumer<Integer[]> {
            private final Consumer<Integer[]> action;

            public Action(Consumer<Integer[]> action) {
                this.action = action;
            }

            public void accept(Integer[] values){
                beginDraw();
                action.accept(values);
                endDraw();
            };
        }

        Map<String, Consumer<Integer[]>> getList() {
            Map<String, Consumer<Integer[]>> m = new LinkedHashMap<>();
            final int ink = Table.INK.ordinal();
            final int paper = Table.PAPER.ordinal();
            m.put("Flip ink", new Action( i -> flipColorCell(Table.INK, i[ink])));
            m.put("Flip paper", new Action( i -> flipColorCell(Table.PAPER, i[paper])));
            m.put("Flip all inks", new Action( dummy -> {
                for (int i = 0; i < palette.getColorsCount(Table.INK); i++) flipColorCell(Table.INK, i);
            }));
            m.put("Flip all papers", new Action( dummy -> {
                for (int i = 0; i < palette.getColorsCount(Table.PAPER); i++) flipColorCell(Table.PAPER, i);
            }));
            m.put("Inverse palette", new Action( dummy -> inverseColors()));
            m.put("Swap ink0 <-> paper0", new Action( c -> swapInkPaper(c[ink], c[paper], 0)));
            m.put("Swap ink1 <-> paper1", new Action( c -> swapInkPaper(c[ink], c[paper], 1)));
            m.put("Correct X mode", (dummy) -> correctXMode());
            m.put("Set ZX-Spectrum palette", new Action(dummy -> setSpectrumPalette()));
            m.put("Set Extended palette", new Action(dummy -> setExtendedPalette()));
            return m;
        }

        private void flipColorCell(Table table, int index) {
            PixelProcessor.PixelDataList l = new PixelProcessor.PixelDataList();
            image.forEachPixel((x, y, b, a) -> {
                l.setPixelData(b);
                for (int i = 0; i < l.size(); i++) {
                    final Integer e = l.get(i);
                    if (table == Table.INK) {
                        if (PixelProcessor.inkFromAttr(a) == index) l.set(i, e ^ ((e & 0x2) >> 1));
                    } else {
                        if (PixelProcessor.paperFromAttr(a) == index) l.set(i, e ^ (((~e) & 0x2) >> 1));
                    }
                }
                return l.getPixelData();
            });
            int c = palette.getColorCell(table, index);
            palette.setColorCell(Palette.combine(Palette.second(c), Palette.first(c)), table, index);
        }

        private void inverseColors() {
            image.forEachPixel((x, y, b, a) -> (byte) (b ^ 0x22));
            image.forEachAttr((x, y, a) -> paperToAttr(inkToAttr((byte) 0, paperFromAttr(a)), inkFromAttr(a)));
            int l = Integer.min(palette.getColorsCount(Table.INK), palette.getColorsCount(Table.PAPER));
            int[] ink = new int[palette.getColorsCount(Table.INK)];
            int[] paper = new int[palette.getColorsCount(Table.PAPER)];
            for (int i = 0; i < ink.length; i++)
                ink[i] = palette.getColorCell(Table.INK, i);
            for (int i = 0; i < paper.length; i++)
                paper[i] = palette.getColorCell(Table.PAPER, i);
            palette.setPalette(paper, ink);
        }

        private void swapInkPaper(int ink, int paper, int shift) {
            Point pos = new Point();
            Pixel ip = new Pixel(Table.INK, ink, shift);
            Pixel pp = new Pixel(Table.PAPER, paper, shift);
            for (int x = 0; x < getImageWidth(); x++) {
                for (int y = 0; y < getImageHeight(); y++) {
                    pos.setLocation(x, y);
                    Pixel pix = getPixel(pos);
                    if (pix.equals(ip)) {
                        setPixel(pp,pos);
                    } else if (pix.equals(pp)) setPixel(ip, pos);
                }
            }
            int i = palette.getColorCell(Table.INK, ink);
            int i1 = Palette.split(i, shift);
            int p = palette.getColorCell(Table.PAPER, paper);
            int p1 = Palette.split(p, shift);
            palette.setColorCell(Palette.replace(i, p1, shift), Table.INK, ink);
            palette.setColorCell(Palette.replace(p, i1, shift), Table.PAPER, paper);
        }

        private void correctXMode() {
            beginDraw();
            Point pos = new Point();
            for (int y = 0; y < getImageHeight(); y++) {
                for (int x = 0; x < getImageWidth(); x += 2) {
                    pos.setLocation(x, y);
                    byte b = getPixelData(pos);
                    int b1 = PixelProcessor.split(b,0);
                    int b2 = PixelProcessor.split(b,1);
                    if ((b1 == 2 && b2 == 1) || (b1 == 1 && b2 == 2)) {
                        putPixelData(pos, PixelProcessor.combine(b2, b1));
                    }
                }

            }
            endDraw();
            fireImageChanged();
        }

    }

}
