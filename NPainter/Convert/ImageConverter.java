package NPainter.Convert;

import NPainter.NPalette;
import NPainter.PixelProcessor;
import Painter.Screen.ImageSupplier;
import Painter.Screen.Palette.ColorConverter;
import Painter.Screen.Palette.Palette;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static NPainter.NScreen.Table.INK;
import static NPainter.NScreen.Table.PAPER;
import static Painter.Screen.Palette.Palette.split;

/**
 * Created by aleksey on 23.01.16.
 * Converter for images into screen format
 */

interface ConvertProgress {
    void run();
    void ended();
}

public class ImageConverter implements ImageSupplier {
    private BufferedImage image = null;
    private ColorConverter converter = new ColorConverter();
    private final int sizeXCells;
    private final int sizeYCells;
    private ColorMatrix colors4Tiles;
    private ImageChangeListener listener = null;
    private Palette palette = new NPalette();
    private boolean isPaletteCalculated = false;

    private ConvertProgress progress = null;
    private Combinator combinator;

    public ImageConverter(BufferedImage image) {
        this.image = image;
        sizeXCells = image.getWidth() / 8;
        sizeYCells = image.getHeight() / 8;
        loadColorMap();
    }

    void setProgress(ConvertProgress progress) {
        this.progress = progress;
    }

    void loadColorMap() {
        converter.getColorMap().clear();
        for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++) {
            converter.fromRGB(new Color(image.getRGB(x,y)));
        }
    }

    private boolean preview = false;

    @Override
    public int getImageWidth() {
        return image.getWidth();
    }

    @Override
    public int getImageHeight() {
        return image.getHeight();
    }

    @Override
    public Color getPixelColor(Point pos) {
        Color color = new Color(image.getRGB(pos.x, pos.y));
        if (preview) return converter.remap(color); else return color;
    }

    @Override
    public void addChangeListener(ImageChangeListener listener) {
        this.listener = listener;
    }

    public boolean getPreview() {
        return this.preview;
    }

    public void setPreview(boolean enabled) {
        this.preview = enabled;
        if (this.listener != null) listener.imageChanged();
    }

    public Map<Color,Integer> getColorMap() {
        return converter.getColorMap();
    }

    Palette getPalette() {
        return palette;
    }

    ColorMatrix getColors4Tiles(int sizeX, int sizeY, BufferedImage img) {
        ColorMatrix c = new ColorMatrix(sizeX, sizeY);
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                Map<Integer, Integer> cnt = new HashMap<>();
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        final int xi = x * 8 + xx, yi = y * 8 + yy;
                        if (xi < img.getWidth() && yi < img.getHeight()) {
                            int i = converter.fromRGB(new Color(img.getRGB(xi, yi)));
                            cnt.merge(i, 1, (o, n) -> o + n);
                        }
                    }
                c.put(x, y, cnt); //.keySet().stream()
                        //.sorted((f, g) -> cnt.get(g) - cnt.get(f))
                        //.limit(4).sorted().collect(Collectors.toList()));
            }
        }
        return c;
    }

    void calcPalette(Palette palette) {
        this.palette = palette;
        calcPalette();
    }

    void calcPalette() {
        isPaletteCalculated = true;
        colors4Tiles = getColors4Tiles(sizeXCells, sizeYCells, image);
        combinator = new Combinator();
        combinator.run(colors4Tiles);
        Timer timer = new Timer(200,e -> {
            combinator.fillPalette(palette);
            if (combinator.isRunning()) {
                if (progress != null) progress.run();
            } else {
                ((Timer)(e.getSource())).stop();
                if (progress != null) progress.ended();
            }
        });
        combinator.fillPalette(palette);
        timer.start();
    }

    void cancel() {
        combinator.stop();
    }

    public DataInputStream asTileStream() throws IOException {

        if (!isPaletteCalculated) calcPalette();

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bs);

        palette.savePalette(os);
        os.writeInt(image.getWidth()/2);
        os.writeInt(image.getHeight());

        for (int x = 0; x < sizeXCells; x++)
            for (int y = 0; y < sizeYCells; y++) {
                final int[] m = palette.findAttr(colors4Tiles.get(x,y));
                final byte attr = PixelProcessor.packAttr(m[0], m[1]);

                int[] l = indexListByAttr(attr);
                for (int yy = 0; yy < 8; yy++)
                    for (int xx = 0; xx < 8; xx+=2) {
                        final int xi = x * 8 + xx;
                        final int yi = y * 8 + yy;
                        final Color color1 = getColor(xi, yi);
                        final Color color2 = getColor(xi+1, yi);
                        os.write(PixelProcessor.combine(Palette.fromRGB(color1, l), Palette.fromRGB(color2, l)));
                    }
                os.write(attr);
            }
        return new DataInputStream(new ByteArrayInputStream(bs.toByteArray()));
    }

    private Color getColor(int x, int y) {
        final int dummyColor = image.getRGB(0, 0);
        final boolean inImg = x < image.getWidth() && y < image.getHeight();
        return converter.remap(new Color(inImg ? image.getRGB(x, y) : dummyColor));
    }

    int[] indexListByAttr(byte attr) {
        int[] l = {-1, -1, -1, -1};
        int ink = palette.getColorCell(INK, PixelProcessor.inkFromAttr(attr));
        int paper = palette.getColorCell(PAPER, PixelProcessor.paperFromAttr(attr));
        final int paperSize = palette.getCellSize(PAPER);
        for (int i = 0; i < paperSize; i++)
            l[i] = split(paper, i);
        for (int i = 0; i < palette.getCellSize(PAPER); i++) //TODO: MAYBE ERROR!!!
            l[i + paperSize] = split(ink, i);
        return l;
    }

}

class ColorMatrix {
    private final int sizeX, sizeY;
    private final ArrayList<List<Integer>> colorIndicesList;
    private final ArrayList<Map<Integer,Integer>> count;
    private int stride;

    ColorMatrix(int x, int y) {
        sizeX = x;
        sizeY = y;
        colorIndicesList = new ArrayList<>(x*y);
        count = new ArrayList<>(x*y);
        IntStream.range(0, x*y).forEach(i -> {
            colorIndicesList.add(null);
            count.add(null);
        });
        stride = y;
    }

    List<Integer> get(int x, int y) {
        return colorIndicesList.get(x * stride + y);
    }

    void put(int x, int y, List<Integer> e) {
        colorIndicesList.set(x * stride + y, e);
    }

    void put(int x, int y, Map<Integer, Integer> cnt) {
        List<Integer> colorIndices = cnt.keySet().stream()
                .sorted((f, g) -> cnt.get(g) - cnt.get(f))
                .limit(4).sorted().collect(Collectors.toList());
        this.put(x, y, colorIndices);
        count.set(x * stride + y, cnt);
    }

    int getCount(int x, int y, int colorIndex) {
        return count.get(x*stride + y).getOrDefault(colorIndex,0);
    }

    void summarize(Consumer<? super List<Integer>> consumer) {
        colorIndicesList.forEach(consumer);
    }

    double summarize(ToDoubleBiFunction<Point, ? super List<Integer>> consumer) {
        Point p = new Point();
        double d = 0;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                p.setLocation(x,y);
                d += consumer.applyAsDouble(p, get(x, y));
            }
        }
        return d;
    }
}



