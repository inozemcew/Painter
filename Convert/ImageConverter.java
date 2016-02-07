package Painter.Convert;

import Painter.ImageSupplier;
import Painter.Palette.ColorConverting;
import Painter.Palette.Palette;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by aleksey on 23.01.16.
 * Converter for images into screen format
 */
public class ImageConverter implements ImageSupplier {
    BufferedImage img = null;
    ColorConverter converter = new ColorConverter();
    private final int sizeXCells;
    private final int sizeYCells;
    private Integer[][][] colors4Tiles;
    private ImageChangeListener listener = null;

    public ImageConverter(BufferedImage img) {
        this.img = img;
        sizeXCells = img.getWidth() / 8;
        sizeYCells = img.getHeight() / 8;
        loadColorMap();
    }

    void loadColorMap() {
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
    public Color getPixelColor(int x, int y) {
        Color color = new Color(img.getRGB(x, y));
        if (preview) return converter.remap(color); else return color;
    }

    @Override
    public void addChangeListener(ImageChangeListener listener) {
        this.listener = listener;
    }

    boolean getPreview() {
        return this.preview;
    }

    void setPreview(boolean enabled) {
        this.preview = enabled;
        if (this.listener != null) listener.imageChanged();
    }

    Map<Color,Integer> getColorMap() {
        return converter.getColorMap();
    }

    protected Integer[][][] getColors4Tiles() {
        Integer c[][][] = new Integer[sizeXCells][sizeYCells][4];
        for (int x = 0; x < sizeXCells; x++)
            for (int y = 0; y < sizeYCells; y++) {
                Arrays.fill(c[x][y], -2);
                Map<Integer, Integer> cnt = new HashMap<>();
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        final int xi = x * 8 + xx, yi = y * 8 + yy;
                        if (xi < img.getWidth() && yi < img.getHeight()) {
                            int i = converter.fromRGB(new Color(img.getRGB(xi, yi)));
                            cnt.merge(i, 1, (o, n) -> o + n);
                        }
                    }
                c[x][y] = Stream.concat(
                            cnt.keySet().stream().sorted((f, g) -> cnt.get(g) - cnt.get(f)),
                            Stream.of(-2, -2, -2, -2))
                        .limit(4).sorted().toArray(Integer[]::new);
            }
        return c;
    }

    public DataInputStream asTileStream() throws IOException {

        colors4Tiles = getColors4Tiles();

        Deque<Integer[]> stat = new LinkedList<>();
        for (int x = 0; x < sizeXCells; x++)
            for (int y = 0; y < sizeYCells; y++) {
                final Integer[] i = colors4Tiles[x][y];
                if (!stat.stream().anyMatch(s -> Arrays.deepEquals(s, i)))
                    stat.add(Arrays.copyOf(i, 4));
            }

        //final int[][] ixs = {{0, 1, 2, 3}, {0, 2, 1, 3}, {0, 3, 1, 2}, {1, 2, 0, 3}, {1, 3, 0, 2}, {2, 3, 0, 1}};

        ArrayList<Pair> pairs = new ArrayList<>();
        for (Integer[] st : stat) {
            for (int j = 0; j < 3; j++)
                for (int k = j + 1; k < 4; k++) {
                    int first = st[j];
                    int second = st[k];
                    if (first == -2 || second == -2) continue;
                    pairs.stream()
                            .filter(item -> item.equals(first, second))
                            .findFirst()
                            .orElseGet(() -> Pair.createAndAdd(first, second, pairs))
                            .inc();
                }
        }
        pairs.sort((Pair f, Pair s) -> s.count - f.count);

        Combinator comb = new Combinator(new ArrayList<>(8), new ArrayList<>(8))
                .next(
                        stat.stream()
                                .sorted((x, y) -> {
                                    int xx = 0, yy = 0;
                                    for (int i = 0; i < 4; i++) {
                                        if (x[i] == -2) xx++;
                                        if (y[i] == -2) yy++;
                                    }
                                    return xx - yy;
                                })
                                .collect(Collectors.toCollection(LinkedList<Integer[]>::new)),
                        pairs);
        if (comb.ink.size()<8) comb.ink.add(new Pair(0,0));

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bs);

        comb.saveInkPaper(os);
        os.writeInt(img.getWidth());
        os.writeInt(img.getHeight());

        for (int x = 0; x < sizeXCells; x++)
            for (int y = 0; y < sizeYCells; y++) {
                final byte attr = comb.getAttr(colors4Tiles[x][y]);

                int[] l = comb.attrToList(attr);
                for (int yy = 0; yy < 8; yy++)
                    for (int xx = 0; xx < 8; xx++) {
                        final int xi = x * 8 + xx;
                        final int yi = y * 8 + yy;
                        final Color color = converter.remap(new Color(
                                (xi < img.getWidth() && yi < img.getHeight())
                                ? img.getRGB(xi, yi) : img.getRGB(0, 0)));
                        os.write ((byte) (Palette.fromRGB(color,l)));
                    }
                os.write(attr);
            }
        return new DataInputStream(new ByteArrayInputStream(bs.toByteArray()));
    }

}

class Pair {
    int first, second, count;

    Pair(int first, int second) {
        this.first = first;
        this.second = second;
        this.count = 0;
    }

    Pair(int first, int second, List<Pair> pairs) {
        this(first, second);
        if (pairs == null) return;
        this.count = pairs.stream().filter(this::equals).map(p -> p.count).findFirst().orElse(0);
    }

    void inc() {
        count++;
    }

    boolean isEmpty() {
        return first == -2 && second == -2;
    }

    Pair ordered() {
        int f, s;
        f = (first == -2) ? second : first;
        s = (second == -2) ? first : second;
        return new Pair(f, s);
    }

    boolean equals(int x, int y) {
        if (this.first == -2 && this.second == -2) return true;
        if (this.first == -2) return (this.second == x || this.second == y);
        if (this.second == -2) return (this.first == x || this.first == y);
        return (this.first == x && this.second == y) || (this.first == y && this.second == x);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) return false;
        Pair o = (Pair) obj;
        return this.equals(o.first, o.second);
    }

    static Pair createAndAdd(int first, int second, ArrayList<Pair> vector) {
        Pair p = new Pair(first, second);
        vector.add(p);
        return p;
    }

    Pair complement(Integer[] s) {
        int[] a = {-2, -2};
        int j = 0;
        for (Integer i : s) {
            if (i != this.first && i != this.second) a[j++] = i;
            if (j > 1) break;
        }
        return new Pair(a[0], a[1]);
    }

    int rank(Integer[] s, List<Pair> ink, List<Pair> paper) {
        if (this.isIn(ink) && this.complement(s).isIn(paper)) return 1;
        if (this.isIn(ink) || this.complement(s).isIn(paper)) return 2;
        return 2;
    }

    boolean isIn(List<Pair> pairs) {
        return pairs.stream().anyMatch(this::equals);
    }

    int compareTo(Pair y, Integer[] s) {
        return (y.count + y.complement(s).count) - (this.count + this.complement(s).count);
    }

    Integer[] asArray() {
        return new Integer[]{first, second};
    }
}

class Combinator {
    List<Pair> ink, paper;
    int tries = 0;
    final int maxTries = 555000;
    int bestCount;
    Combinator best;

    Combinator(List<Pair> ink, List<Pair> paper) {
        this.ink = new ArrayList<>(ink);
        this.paper = new ArrayList<>(paper);
        this.bestCount = Integer.MAX_VALUE;
        this.best = this;
    }

    List<Pair> getPairs(Integer[] s, List<Pair> pairs) {
        List<Pair> ps = new ArrayList<>();
        for (int j = 0; j < 3; j++)
            for (int k = j + 1; k < 4; k++) {
                //if (s[j] == -2 && s[k] == -2) continue;
                ps.add(new Pair(s[j], s[k], pairs));
            }
        Map<Integer, List<Pair>> m = ps.stream().collect(Collectors.groupingBy(p -> p.rank(s, ink, paper)));
        if (m.containsKey(0)) return m.get(0);
        if (m.containsKey(1)) return m.get(1);
        else return Stream.concat(
                /*m.containsKey(1) ? m.get(1).stream() :*/ Stream.empty(),
                m.containsKey(2) ? m.get(2).stream().sorted((x, y) -> x.compareTo(y, s)) : Stream.empty()
        ).collect(Collectors.toList());
    }

    void addTo(List<Pair> list, Pair p) {
        if (p.isEmpty()) return;
        if (!list.contains(p)) {
            final Pair ordered = p.ordered();
            if (p.first == -2 || p.second == -2) {
                Optional<Pair> s = list.stream().filter(x -> x.first == x.second).findAny();
                if (s.isPresent()) {
                    s.get().second = ordered.first;
                    return;
                }
            }
            list.add(ordered);
        }
    }

    Combinator next(Deque<Integer[]> stat, final List<Pair> pairs) {
        if (stat.isEmpty()) return this;
        final Integer[] s = stat.pop();
        for (Pair p : getPairs(s, pairs)) {
            Combinator n = new Combinator(this.ink, this.paper);
            n.tries = tries;
            n.bestCount = bestCount;
            n.addTo(n.ink, p);
            n.addTo(n.paper, p.complement(s));
            if (n.ink.size() <= 8 && n.paper.size() <= 8) {
                Combinator r = n.next(stat, pairs);
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
        if (stat.size() < bestCount) {
            bestCount = stat.size();
            best = this;
        }
        stat.push(s);
        return null;
    }

    byte getAttr(Integer[] s) {
        int bestN = -1;
        byte best = 0;
        for (int i = 0; i < ink.size(); i++) {
            final Pair ip = ink.get(i); //(i<ink.size()) ? ink.get(i) : new Pair(0,0);
            for (int p = 0; p < paper.size(); p++) {
                final Pair pp = paper.get(p);
                int n = 0;
                for (int k : s) {
                    if (k == ip.first || k == ip.second || k == pp.first || k == pp.second || k == -2)
                        n++;
                }
                final byte b = (byte) (i + (p << 3));
                if (n == s.length) return b;
                if (n > bestN) {
                    bestN = n;
                    best = b;
                }
            }
        }
        return best;
    }

    void saveInkPaper(DataOutputStream stream) throws IOException {
        Pair j;
        for (int i = 0; i < 8; i++) {
            j = (i < ink.size()) ? ink.get(i) : new Pair(0, 0);
            stream.writeInt(Palette.combine(j.first, j.second));
        }
        for (int i = 0; i < 8; i++) {
            j = (i < paper.size()) ? paper.get(i) : new Pair(0, 0);
            stream.writeInt(Palette.combine(j.first, j.second));
        }
    }

    int[] attrToList(byte attr) {
        int[] l = {-1,-1,-1,-1};
        if (paper.size()>0) {
            final Pair p = paper.get((attr >> 3) & 7);
            l[0] = p.first;
            l[1] = p.second;
        }
        if (ink.size()>0) {
            final Pair i = ink.get(attr & 7);
            l[2] = i.first;
            l[3] = i.second;
        }
        return l;
    }
}

class ColorConverter implements ColorConverting {
    private HashMap<Color,Integer> cache = new HashMap<>();

    @Override
    public int fromRGB(Color color) {
        return cache.computeIfAbsent(color, Palette::fromRGB);
    }
    Color remap (Color color) {
        return Palette.toRGB(fromRGB(color));
    }

    void replace (Color color, int index) {
        cache.replace(color,index);
    }

    Map<Color,Integer> getColorMap() {
        return cache;
    }
}