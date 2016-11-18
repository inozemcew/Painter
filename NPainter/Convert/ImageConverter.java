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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static NPainter.NScreen.Table.INK;
import static NPainter.NScreen.Table.PAPER;
import static Painter.Screen.Palette.Palette.split;

/**
 * Created by aleksey on 23.01.16.
 * Converter for images into screen format
 */
public class ImageConverter implements ImageSupplier {
    BufferedImage image = null;
    ColorConverter converter = new ColorConverter();
    private final int sizeXCells;
    private final int sizeYCells;
    private ColorMatrix colors4Tiles;
    private ImageChangeListener listener = null;
    private Palette palette = new NPalette();
    private boolean isPaletteCalculated = false;

    public ImageConverter(BufferedImage image) {
        this.image = image;
        sizeXCells = image.getWidth() / 8;
        sizeYCells = image.getHeight() / 8;
        loadColorMap();
    }

    public void loadColorMap() {
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
                c.put(x, y, cnt.keySet().stream()
                        .sorted((f, g) -> cnt.get(g) - cnt.get(f))
                        .limit(4).sorted().collect(Collectors.toList()));
            }
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
                            .orElseGet(() -> { allPairs.add(c); return c;})
                            .inc());
        }
        allPairs.sort((ColorCell f, ColorCell s) -> s.getCount() - f.getCount());
        //allPairs.forEach(p -> System.out.println(p.toString()));
        return allPairs;
    }


    void calcPalette(Palette palette) {
        this.palette = palette;
        calcPalette();
    }


    void calcPalette() {

        colors4Tiles = getColors4Tiles(sizeXCells, sizeYCells, image);

        Deque<List<Integer>> stat = Combinator.getStat(colors4Tiles);

        List<ColorCell> colorCells = getAllPairs(stat);

        ComplementMaps complementMaps = Combinator.createComplementMaps(stat, colorCells);

        Combinator comb = new Combinator(new HashSet<>(8), new HashSet<>(8), complementMaps);
        Combinator ncomb = comb.next(
                stat, /*.stream()
                        .sorted((x, y) -> y.size() - x.size())
                        .collect(Collectors.toCollection(LinkedList<List<Integer>>::new)),*/
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

class ColorMatrix extends ArrayList<List<Integer>> {
    private int xs,ys;

    ColorMatrix(int x, int y) {
        super(x*y);
        IntStream.range(0, x*y).forEach(i -> add(null));
        xs = x;
        ys = y;
    }

    List<Integer> get(int x, int y) {
        return super.get(x * ys + y);
    }

    void put(int x, int y, List<Integer> e) {
        super.set(x * ys + y, e);
    }
}

class ComplementMaps extends HashMap<List<Integer>, Map<ColorCell,ColorCell>> { };

class Combinator {
    private Set<ColorCell> ink, paper;
    private int tries = 0;
    private static final int maxTries = 555000;
    private int bestCount;
    Combinator best;

    private ComplementMaps complementMaps;

    final int SIZE  = 8; //TODO: rework!

    Combinator(Set<ColorCell> ink, Set<ColorCell> paper, ComplementMaps complementMaps) {
        this.ink = new HashSet<>(ink);
        this.paper = new HashSet<>(paper);
        this.complementMaps = complementMaps;
        this.bestCount = Integer.MAX_VALUE;
        this.best = this;
    }

    static Deque<List<Integer>> getStat(ColorMatrix tilesColors) {
        Map<Set<Integer>, Integer> count = new HashMap<>();
        tilesColors.forEach(l -> {
            if (l != null) {
            Set<Integer> s = new HashSet<>(l);
            Optional<Set<Integer>> c = count.keySet().stream().filter(i -> i.containsAll(s)).findAny();
            if (c.isPresent())
                count.merge(c.get(), 1, (o, n) -> o + n);
            else {
                int cnt = 1;
                List<Set<Integer>> cc = count.keySet().stream().filter(i -> s.containsAll(i)).collect(Collectors.toList());
                for(Set<Integer> i : cc) {
                    cnt += count.get(i);
                    count.remove(i);
                }
                count.put(s, cnt);
            }
        }});
        count.forEach((k,v) -> System.out.println(
                k.stream().map(Object::toString).collect(Collectors.joining(",")) + " = " + v)
            );
        return count.keySet().stream()
                .sorted((x, y) -> count.get(y) - count.get(x))
                .map(ArrayList::new)
                .collect(Collectors.toCollection(LinkedList<List<Integer>>::new));
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
        if (st.size()<4) st.forEach(i -> b.accept(new ColorCell(colorCells, i)));
        if (st.size()<3) b.accept(new ColorCell());
        return b.build();
    }

    static ComplementMaps createComplementMaps(Collection<List<Integer>> stat, List<ColorCell> pairs) {
        ComplementMaps m = new ComplementMaps();
        for (List<Integer> st : stat) {
            m.put(st, Combinator.getColorPairsStream(st, pairs)
                    .collect(HashMap<ColorCell, ColorCell>::new,
                            (h,p) -> h.put(p, p.complement(st, pairs)),
                            HashMap::putAll));
        }
        return m;
    }

    static int rank(ColorCell cell, Map<ColorCell, ColorCell> map, Collection<ColorCell> ink, Collection<ColorCell> paper) {
        final ColorCell complement = map.get(cell);
        final boolean b1 = cell.getSize() < 2;
        final boolean inkContains = ink.stream().anyMatch(c-> c.contains(cell) || (b1 && c.getSize()<2));
        final boolean b2 = complement.getSize() < 2;
        final boolean paperContains = paper.stream().anyMatch(c -> c.contains(complement) || (b2 && c.getSize()<2));
        if (inkContains && paperContains) return 0;
        if (inkContains || paperContains) return 1;
        return 2;
    }

    int compareTo(ColorCell x, ColorCell y, Map<ColorCell, ColorCell> complements) {
        return (y.getCount() + complements.get(y).getCount()) - (x.getCount() + complements.get(x).getCount());
    }


    List<ColorCell> sortPairs(Map<ColorCell, ColorCell> map) {
        Stream<ColorCell> ps = map.keySet().stream();
        Map<Integer, List<ColorCell>> m = ps.collect(Collectors.groupingBy(p -> rank(p, map, ink, paper)));
        if (m.containsKey(0))
            return m.get(0);
        if (m.containsKey(1))
            return m.get(1).stream().sorted((x,y) -> y.asArray().length - x.asArray().length).collect(Collectors.toList());
        if (m.containsKey(2))
            return m.get(2).stream().sorted((x, y) -> compareTo(x, y, map)).collect(Collectors.toList());
        return new ArrayList<>();
    }

    static void addTo(Set<ColorCell> set, ColorCell p) {
        if (p.isEmpty()) return;
        if (!set.contains(p)) {
            if (set.stream().anyMatch(i -> i.contains(p))) return;
            set.removeIf(p::contains);
            if (p.getSize()<2) {
                Optional<ColorCell> p2 = set.stream().filter(c -> c.getSize() < 2).findAny();
                if (p2.isPresent()) {
                    p2.get().merge(p);
                    return;
                }
            }
            set.add(p);
        }
    }

    Combinator next(Deque<List<Integer>> uniqueColorsStack, final List<ColorCell> colorCells) {
        if (uniqueColorsStack.isEmpty()) return this;
        final List<Integer> s = uniqueColorsStack.pop();
        final Map<ColorCell, ColorCell> map = complementMaps.get(s);
        for (ColorCell p : sortPairs(map) ) {
            Combinator n = new Combinator(this.ink, this.paper, this.complementMaps);
            n.tries = tries;
            n.bestCount = bestCount;
            n.addTo(n.ink, p);
            n.addTo(n.paper, map.get(p));
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

