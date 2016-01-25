package Painter;

import Painter.Palette.ColorConverting;
import Painter.Palette.Palette;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ainozemtsev on 23.11.15.
 */
public class ImageBuffer {
    public static final int SIZE_X = 320;
    public static final int SIZE_Y = 240;
    //public static final Dimension SIZE = new Dimension(SIZE_X, SIZE_Y);
    public static final int ATTR_SIZE_X = SIZE_X / 8;
    public static final int ATTR_SIZE_Y = SIZE_Y / 8;

    private byte pixbuf[][] = new byte[SIZE_X][SIZE_Y];
    private byte attrbuf[][] = new byte[ATTR_SIZE_X][ATTR_SIZE_Y];


    public byte getPixel(int x, int y) {
        return (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y) ? pixbuf[x][y] : -1;
    }

    public byte getAttr(int x, int y) {
        return attrbuf[x / 8][y / 8];
    }

    void putPixel(int x, int y, byte pixel, byte attr) {
        this.pixbuf[x][y] = pixel;
        this.attrbuf[x / 8][y / 8] = attr;
    }

    void store(OutputStream stream) throws IOException {
        store(stream, 0, 0, SIZE_X, SIZE_Y);
    }

    void store(OutputStream stream, int x, int y, int width, int height) throws IOException {
        for (int i = x; i < Integer.min(x + width, SIZE_X); i++)
            stream.write(pixbuf[i], y, height);
        for (int i = x / 8; i < Integer.min((x + width) / 8, ATTR_SIZE_X); i++)
            stream.write(attrbuf[i], y / 8, height / 8);
    }

    void load(InputStream stream) throws IOException {
        load(stream, 256, 192);
    }

    void load(InputStream stream, int width, int height) throws IOException {
        int ox = (SIZE_X - width) / 2;
        int oy = (SIZE_Y - height) / 2;
        load(stream, ox, oy, width, height);
    }

    void load(InputStream stream, int ox, int oy, int width, int height) throws IOException {
        byte[] b = new byte[height];
        for (int i = ox; i < width + ox; i++) {
            for (int v = 0; v < height; v += stream.read(b, v, height - v)) ;
            if (i >= 0 && i < SIZE_X) {
                for (int j = oy; j < height + oy; j++) {
                    if (j >= 0 && j < SIZE_Y) pixbuf[i][j] = b[j - oy];
                }
            }
        }

        byte[] a = new byte[height / 8];
        for (int i = ox / 8; i < (ox + width) / 8; i++) {
            final int h = height / 8;
            for (int v = 0; v < h; v += stream.read(a, v, h - v)) ;
            if (i >= 0 && i < ATTR_SIZE_X) {
                for (int j = oy / 8; j < (height + oy) / 8; j++) {
                    if (j >= 0 && j < ATTR_SIZE_Y) attrbuf[i][j] = a[j - oy / 8];
                }
            }
        }
    }

    void loadByTiles(InputStream stream, int sx, int sy, int width, int height) throws IOException {
        for (int x = sx; x < sx + width; x++)
            for (int y = sy; y < sy + height; y++) {

                for (int yy = 0; yy < 8; yy++)
                    for (int xx = 0; xx < 8; xx++) {
                        final int xi = x * 8 + xx;
                        final int yi = y * 8 + yy;
                        if (xi < SIZE_X && yi < SIZE_Y)
                            pixbuf[(xi)][(yi)] = (byte) stream.read();
                    }
                if (x < ATTR_SIZE_X && y < ATTR_SIZE_Y)
                    attrbuf[x][y] = (byte) stream.read();
            }
    }

    public int[][] importPNG(InputStream stream) throws IOException {
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

            Pair add(ArrayList<Pair> vector) {
                vector.add(this);
                return this;
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
                if (this.isIn(ink) && this.complement(s).isIn(paper)) return 0;
                if (this.isIn(ink) || this.complement(s).isIn(paper)) return 1;
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
                else return Stream.concat(
                        m.containsKey(1) ? m.get(1).stream() : Stream.empty(),
                        m.containsKey(2) ? m.get(2).stream().sorted((x, y) -> x.compareTo(y, s)) : Stream.empty()
                ).collect(Collectors.toList());
                /*ps = Stream.concat(
                        Stream.concat(
                                m.containsKey(0) ? m.get(0).stream() : Stream.empty(),
                                m.containsKey(1) ? m.get(1).stream() : Stream.empty()),
                        m.containsKey(2) ? m.get(2).stream().sorted((x, y) -> x.compareTo(y, s)) : Stream.empty())
                        .collect(Collectors.toList());
                return ps; */
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
                    final Pair ip = ink.get(i);
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

            int[][] getInkPaper() {
                int[][] p = new int[2][8];
                Pair j;
                for (int i = 0; i < 8; i++) {
                    j = (i < ink.size()) ? ink.get(i) : new Pair(0, 0);
                    p[0][i] = Palette.combine(j.first, j.second);
                    j = (i < paper.size()) ? paper.get(i) : new Pair(0, 0);
                    p[1][i] = Palette.combine(j.first, j.second);
                }
                return p;
            }

            List<Integer> attrToList(byte attr) {
                List<Integer> l = new ArrayList<>();
                final Pair p = paper.get((attr >> 3) & 7);
                l.add(p.first);
                l.add(p.second);
                final Pair i = ink.get(attr & 7);
                l.add(i.first);
                l.add(i.second);
                return l;
            }
        }

        ColorConverting converter = Palette.createConverter();

        BufferedImage png = ImageIO.read(stream);
        //if (png.getWidth() < 256 || png.getHeight() < 192) throw new IOException("Wrong png size");
        Integer c[][][] = new Integer[ATTR_SIZE_X][ATTR_SIZE_Y][4];
        for (int x = 0; x < ATTR_SIZE_X; x++)
            for (int y = 0; y < ATTR_SIZE_Y; y++) {
                Arrays.fill(c[x][y], -2);
                Map<Integer, Integer> cnt = new HashMap<>();
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        final int xi = x * 8 + xx, yi = y * 8 + yy;
                        if (xi < png.getWidth() && yi < png.getHeight()) {
                            int i = converter.fromRGB(new Color(png.getRGB(xi, yi)));
                            cnt.merge(i, 1, (o, n) -> o + n);
                        }
                    }
                c[x][y] = Stream.concat(cnt.keySet().stream().sorted((f, g) -> cnt.get(g) - cnt.get(f)), Stream.of(-2, -2, -2, -2))
                        .limit(4).toArray(Integer[]::new);
                Arrays.sort(c[x][y]); //,(a,b)-> b-a);
            }

        Deque<Integer[]> stat = new LinkedList<>();
        for (int x = 0; x < ATTR_SIZE_X; x++)
            for (int y = 0; y < ATTR_SIZE_Y; y++) {
                final Integer[] i = c[x][y];
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
                            .orElseGet(() -> new Pair(first, second).add(pairs)).inc();
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


        for (int x = 0; x < ATTR_SIZE_X; x++)
            for (int y = 0; y < ATTR_SIZE_Y; y++) {
                final byte attr = comb.getAttr(c[x][y]);
                attrbuf[x][y] = attr;
                List<Integer> l = comb.attrToList(attr);
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        final int xi = x * 8 + xx;
                        final int yi = y * 8 + yy;
                        final int color = (xi < png.getWidth() && yi < png.getHeight())
                                ? converter.fromRGB(new Color(png.getRGB(xi, yi)))
                                : converter.fromRGB(new Color(png.getRGB(0, 0)));
                        final byte b = (byte) (l.indexOf(color));
                        pixbuf[(xi)][(yi)] = (b >= 0) ? b : 0;
                    }
            }
        return comb.getInkPaper();
    }
}
