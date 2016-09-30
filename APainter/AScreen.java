package APainter;

import Painter.Palette.Palette;
import Painter.Screen.PixelProcessing;
import Painter.Screen.Screen;
import Painter.SpectrumScreen;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ainozemtsev on 21.06.16.
 */
public class AScreen extends Screen {
    enum Table {Fore, Back}

    private Font font;
    private int fontHeight = 4;
    final static int colors[] = {0x00, 0x24, 0x28, 0x2e, 0x2c, 0x2a, 0x26, 0x22};

    AScreen() {
        this(384, 208);
    }

    AScreen(int w, int h) {
        super(w, h);
        font = Font.getFont();
        int p[] = {0, 0, 0, 0, 0, 0, 0, 0};
        palette.setPalette(colors, p);
    }

    @Override
    final protected void setFactors() {
        GRID_FACTOR.setSize(6,4);
        pixelFactor.setSize(6,4);
        attrFactor.setSize(6,4);
    }

    @Override
    protected Palette createPalette() {
        final int[] tSize = {8, 1};
        final int[] cSize = {1, 1};
        return new Palette(2, tSize, cSize);
    }

    @Override
    protected PixelProcessing createPixelProcessor() {
        return PixelProcessor.createPixelProcessor();
    }

    /*
        @Override
        protected byte attrFromDesc(Pixel pixel, byte oldAttr) {
            return (pixel.table == Table.Fore) ? (byte) pixel.index : oldAttr;
        }

        @Override
        public Color getPixelColor(int x, int y) {
            Pixel p = getPixelDescriptor(x, y);
            return (p.table == Table.Back) ? Color.BLACK : palette.getRGBColor(Table.Fore, p.index, 0);
        }
    */
    @Override
    public Status getStatus(Point pos) {
        return Status.Normal;
    }
/*
    @Override
    public Pixel getPixelDescriptor(int x, int y) {
        final int xx = x / pixelFactor.width;
        final int yy = y / pixelFactor.height;

        byte b = getPixelData(xx, yy);
        byte c = font.getRasterLine(b, y % fontHeight);
        byte attr = getAttr(xx, yy);
        boolean isPaper = ((c & (32 >> (x % 6))) == 0);
        if (isPaper) return new Pixel(Table.Back, 0, 0);
        return new Pixel(Table.Fore, attr, 0);
    }
*/
/*
    @Override
    protected byte pixelFromDesc(Pixel pixel, byte oldPixel, int x, int y) {
        int b = oldPixel;
        if (oldPixel >= 64) b = 0;
        final int m = 1 << (x / 2 % 3 + y / 2 % 2 * 3);
        if (pixel.table == Table.Fore) b = b | m;
        else b = b & (255 - m);
        return (byte) b;
    }
*/
    @Override
    public Enum mapColorTable(int table) {
        return (table == 0) ? Table.Fore : Table.Back;
    }

    @Override
    protected int getPointSize() {
        return 2;
    }

    @Override
    public Map<String, Dimension> getResolutions() {
        HashMap<String, Dimension> m = new HashMap<>();
        m.put("192x104", new Dimension(384, 208));
        m.put("256x192", new Dimension(512, 384));
        return m;
    }

    @Override
    public FileNameExtensionFilter getFileNameExtensionFilter() {
        return new FileNameExtensionFilter("Apogee screen", "scra");
    }


    private void importSCR(SpectrumScreen scr) throws IOException {
        byte rev[] = {0, 4, 2, 6, 1, 5, 3, 7};
        int[] bs = new int[6];
        //scr.setOffset(8,8);
        for (int y = 0; y < image.SIZE_Y; y++) {
            for (int x = 0; x < image.SIZE_X; x++) {
                for (int yy = 0; yy < 2; yy++)
                    for (int xx = 0; xx < 3; xx++) {
                        final SpectrumScreen.Pixel p = scr.getPixel(x * 3 + xx, y * 2 + yy);
                        final int v = (p.value) ? p.ink : p.paper;
                        bs[yy * 3 + xx] = (v < p.ink || v < p.paper) ? 0 : v;
                    }
                int max = Arrays.stream(bs).max().getAsInt();
                int b = 0;
                for (int i = 0; i < 6; i++) {
                    b = (b << 1) | (bs[5 - i] > 0 ? 1 : 0);
                }
                image.putPixel(x, y, (byte) b, (byte) max);
            }
        }
    }

    @Override
    public void importSCR(InputStream stream) throws IOException {
        SpectrumScreen scr = new SpectrumScreen(stream);
        importSCR(scr);
    }
}

