package NPainter.Convert;

import NPainter.NPalette;
import NPainter.PixelProcessor;
import Painter.Screen.ImageSupplier;
import Painter.Screen.Palette.ColorConverter;
import Painter.Screen.Palette.Palette;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static NPainter.NScreen.Table.INK;
import static NPainter.NScreen.Table.PAPER;
import static Painter.Screen.Palette.Palette.split;

/**
 * Created by aleksey on 23.01.16.
 * Converter for images into screen format
 */
public class ImageConverter implements ImageSupplier {
    BufferedImage img = null;
    ColorConverter converter = new ColorConverter();
    private final int sizeXCells;
    private final int sizeYCells;
    private Map<Point, List<Integer>> colors4Tiles;
    private ImageChangeListener listener = null;
    private Palette palette = new NPalette();
    private boolean isPaletteCalculated = false;

    public ImageConverter(BufferedImage img) {
        this.img = img;
        sizeXCells = img.getWidth() / 8;
        sizeYCells = img.getHeight() / 8;
        loadColorMap();
    }

    public void loadColorMap() {
        converter.getColorMap().clear();
        for (int x = 0; x < img.getWidth(); x++) for (int y = 0; y < img.getHeight(); y++) {
            converter.fromRGB(new Color(img.getRGB(x,y)));
        }
    }

    private boolean preview = false;

    @Override
    public int getImageWidth() {
        return img.getWidth();
    }

    @Override
    public int getImageHeight() {
        return img.getHeight();
    }

    @Override
    public Color getPixelColor(Point pos) {
        Color color = new Color(img.getRGB(pos.x, pos.y));
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

    private Map<Point,List<Integer>> getColors4Tiles(BufferedImage img) {
        Map<Point,List<Integer>> c = new HashMap<>();
        Map<Integer, Integer> cnt = new HashMap<>();
        for (int x = 0; x < sizeXCells; x++) for (int y = 0; y < sizeYCells; y++) {
            Point p = new Point(x,y);
            List<Integer> l = new ArrayList<>();
            cnt.clear();
            for (int xx = 0; xx < 8; xx++) for (int yy = 0; yy < 8; yy++) {
                final int xi = x * 8 + xx, yi = y * 8 + yy;
                if (xi < img.getWidth() && yi < img.getHeight()) {
                    int i = converter.fromRGB(new Color(img.getRGB(xi, yi)));
                    cnt.merge(i, 1, (o, n) -> o + n);
                }
            }
            c.put(p,cnt.keySet().stream().sorted((f, g) -> cnt.get(g) - cnt.get(f)).collect(Collectors.toList()));
        }
        return c;
    }

    private Deque<List<Integer>> getStat(Map<Point, List<Integer>> tilesColors) {
        Map<List<Integer>, Integer> count = new HashMap<>();
        tilesColors.values().forEach(l -> count.merge(l,1,(o, n) -> o + n));
        return count.keySet().stream()
                .sorted((x, y) -> count.get(y) - count.get(x))
                .collect(Collectors.toCollection(LinkedList<List<Integer>>::new));
    }

    Map<Point, List<Integer>> getColors4Tiles() {
        Map<Point, List<Integer>> c = new HashMap<>();
        for (int x = 0; x < sizeXCells; x++)
            for (int y = 0; y < sizeYCells; y++) {
                Map<Integer, Integer> cnt = new HashMap<>();
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        final int xi = x * 8 + xx, yi = y * 8 + yy;
                        if (xi < img.getWidth() && yi < img.getHeight()) {
                            int i = converter.fromRGB(new Color(img.getRGB(xi, yi)));
                            cnt.merge(i, 1, (o, n) -> o + n);
                        }
                    }
                Point p = new Point(x,y);
                c.put(p, cnt.keySet().stream()
                        .sorted((f, g) -> cnt.get(g) - cnt.get(f))
                        .limit(4).sorted().collect(Collectors.toList()));
            }
        return c;
    }

    private List<ColorCell> getAllPairs(Deque<List<Integer>> stat) {
        List<ColorCell> allPairs = new ArrayList<>();
        for (List<Integer> st : stat) {
            Combinator.getColorPairsStream(st, null).forEach(c ->
                    allPairs.stream()
                            .filter(cell -> cell.contains(c))
                            .findFirst()
                            .orElseGet(() -> {allPairs.add(c); return c;})
                            .inc());
        }
        allPairs.sort((ColorCell f, ColorCell s) -> s.getCount() - f.getCount());
        return allPairs;
    }


    void calcPalette(Palette palette) {
        this.palette = palette;
        calcPalette();
    }

    void calcPalette() {

        colors4Tiles = getColors4Tiles();

        Deque<List<Integer>> stat = getStat(colors4Tiles);

        //final int[][] ixs = {{0, 1, 2, 3}, {0, 2, 1, 3}, {0, 3, 1, 2}, {1, 2, 0, 3}, {1, 3, 0, 2}, {2, 3, 0, 1}};

        List<ColorCell> colorCells = getAllPairs(stat);

        Combinator comb = new Combinator(new HashSet<>(8), new HashSet<>(8));
        Combinator ncomb = comb.next(
                stat.stream()
                        .sorted((x, y) -> y.size() - x.size())
                        .collect(Collectors.toCollection(LinkedList<List<Integer>>::new)),
                colorCells);
        comb = (ncomb != null) ? ncomb : comb.best;
        comb.fillPalette(palette);
        isPaletteCalculated = true;
    }

    public DataInputStream asTileStream() throws IOException {

        if (!isPaletteCalculated) calcPalette();

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bs);

        palette.savePalette(os);
        os.writeInt(img.getWidth()/2);
        os.writeInt(img.getHeight());

        Point p = new Point();
        for (int x = 0; x < sizeXCells; x++)
            for (int y = 0; y < sizeYCells; y++) {
                p.setLocation(x, y);
                final int[] m = palette.findAttr(colors4Tiles.get(p));
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
        final int dummyColor = img.getRGB(0, 0);
        final boolean inImg = x < img.getWidth() && y < img.getHeight();
        return converter.remap(new Color(inImg ? img.getRGB(x, y) : dummyColor));
    }

    public int[] indexListByAttr(byte attr) {
        int[] l = {-1, -1, -1, -1};
        int ink = palette.getColorCell(INK, PixelProcessor.inkFromAttr(attr));
        int paper = palette.getColorCell(PAPER, PixelProcessor.paperFromAttr(attr));
        final int paperSize = palette.getCellSize(PAPER);
        for (int i = 0; i < paperSize; i++)
            l[i] = split(paper, i);
        for (int i = 0; i < palette.getCellSize(PAPER); i++)
            l[i + paperSize] = split(ink, i);
        return l;
    }

}

class Combinator {
    Set<ColorCell> ink, paper;
    int tries = 0;
    final int maxTries = 555000;
    int bestCount;
    Combinator best;

    final int SIZE  = 8; //TODO: rework!

    Combinator(Set<ColorCell> ink, Set<ColorCell> paper) {
        this.ink = new HashSet<>(ink);
        this.paper = new HashSet<>(paper);
        this.bestCount = Integer.MAX_VALUE;
        this.best = this;
    }

    static Stream<ColorCell> getColorPairsStream(List<Integer> st, Collection<ColorCell> colorCells) {
        Stream.Builder<ColorCell> b = Stream.builder();
        for (int j = 0; j < st.size() - 1; j++) {
            Integer first = st.get(j);
            for (int k = j + 1; k < st.size(); k++) {
                Integer second = st.get(k);
                b.accept(new ColorCell(colorCells, first, second));
            }
        }
        if (st.size()<4) st.forEach(i -> b.accept(new ColorCell(i)));
        if (st.size()<3) b.accept(new ColorCell());
        return b.build();
    }

    List<ColorCell> getPairs(List<Integer> colorsInTile, List<ColorCell> colorCells) {
        Stream<ColorCell> ps = getColorPairsStream(colorsInTile, colorCells);
        Map<Integer, List<ColorCell>> m = ps.collect(Collectors.groupingBy(p -> p.rank(colorsInTile, ink, paper)));
        if (m.containsKey(0))
            return m.get(0);
        if (m.containsKey(1))
            return m.get(1).stream().sorted((x,y) -> y.asArray().length - x.asArray().length).collect(Collectors.toList());
        if (m.containsKey(2))
            return m.get(2).stream().sorted((x, y) -> x.compareTo(y, colorsInTile)).collect(Collectors.toList());
        return new ArrayList<>();
    }

    void addTo(Set<ColorCell> set, ColorCell p) {
        if (p.isEmpty()) return;
        if (!set.contains(p)) {
            if (set.stream().anyMatch(i -> i.contains(p))) return;
            set.removeIf(c -> p.contains(c));
            set.add(p);
        }
    }

    Combinator next(Deque<List<Integer>> uniqueColorsStack, final List<ColorCell> colorCells) {
        if (uniqueColorsStack.isEmpty()) return this;
        final List<Integer> s = uniqueColorsStack.pop();
        for (ColorCell p : getPairs(s, colorCells)) {
            Combinator n = new Combinator(this.ink, this.paper);
            n.tries = tries;
            n.bestCount = bestCount;
            n.addTo(n.ink, p);
            n.addTo(n.paper, p.complement(s));
            if (n.ink.size() <= SIZE && n.paper.size() <= SIZE) {
                Combinator r = n.next(uniqueColorsStack, colorCells);
                if (r != null) return r;
                tries = n.tries;
                if (n.bestCount < bestCount) {
                    best = n.best;
                    bestCount = n.bestCount;
                }
            } else {
                tries++;
                if (tries >= maxTries) return best;
            }
        }
        if (uniqueColorsStack.size() < bestCount) {
            bestCount = uniqueColorsStack.size();
            best = this;
        }
        uniqueColorsStack.push(s);
        return null;
    }

    void fillPalette(Palette palette) {
        int[] ink =Arrays.copyOf(this.ink.stream().mapToInt(c -> Palette.combine(c.asArray())).toArray(),SIZE);
        int[] paper =Arrays.copyOf(this.paper.stream().mapToInt(c -> Palette.combine(c.asArray())).toArray(),SIZE);
        palette.setPalette(ink, paper);
    }
}

