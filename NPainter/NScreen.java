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
        byte attr = getAttr(pos);
        byte pix = getPixelData(pos);
        Pixel pixel = pixelProcessor.unpackPixel(pix, attr, pos);
        return pixelProcessor.getPixelStatus(pixel, enhancedColors);
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

        int[] ink = {0x00, 0x14, 0x18, 0x1e, 0x1c, 0x1a, 0x16, 0x12};
        for (int i = 1; i < 8; i++) ink[i] += (ink[i] + 0x10) << 6;
        palette.setPalette(ink, ink);

    }

    @Override
    public void load(InputStream stream, boolean old) throws IOException, ClassNotFoundException {
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
            if (x<255) {
                stream.reset();
                super.load(stream, old);
                return;
            }
            y = iStream.readInt();
            img = new ImageBuffer(x, y, new Dimension(1, 1), new Dimension(8, 8));
            img.load(iStream, x, y);
        }
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

        super.load(new ByteArrayInputStream(bs.toByteArray()), false);
        stream.close();
    }

    SpecialMethods specialMethods = new SpecialMethods();

    @Override
    public Map<String, Consumer<Integer[]>> getSpecialMethods() {
        return specialMethods.getList();
    }

    private class SpecialMethods {

        Map<String, Consumer<Integer[]>> getList() {
            Map<String, Consumer<Integer[]>> m = new HashMap<>();
            final int ink = Table.INK.ordinal();
            final int paper = Table.PAPER.ordinal();
            m.put("Flip ink", (i) -> flipColorCell(Table.INK, i[ink]));
            m.put("Flip paper", (i) -> flipColorCell(Table.PAPER, i[paper]));
            m.put("Flip all inks", (dummy) -> {
                for (int i = 0; i < palette.getColorsCount(Table.INK); i++) flipColorCell(Table.INK, i);
            });
            m.put("Flip all papers", (dummy) -> {
                for (int i = 0; i < palette.getColorsCount(Table.PAPER); i++) flipColorCell(Table.PAPER, i);
            });
            m.put("Inverse palette", (dummy) -> inverseColors());
            m.put("Swap ink0 <-> paper0", (c) -> swapInkPaper(c[ink], c[paper], 0));
            m.put("Swap ink1 <-> paper1", (c) -> swapInkPaper(c[ink], c[paper], 1));
            m.put("Correct X mode", (dummy) -> correctXMode());
            return m;
        }

        private void flipColorCell(Table table, int index) {
            beginDraw();
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
            endDraw();
        }

        private void inverseColors() {
            beginDraw();
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
            endDraw();
        }

        private void swapInkPaper(int ink, int paper, int shift) {
            beginDraw();
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
            endDraw();
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
