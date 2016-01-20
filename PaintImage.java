package Painter;

import Painter.Palette.ColorConverter;
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
public class PaintImage {
    private byte pixbuf[][] = new byte[256][192];
    private byte attrbuf[][] = new byte[32][24];
    private final UndoRedo undo = new UndoRedo();

    private interface PaintPixel {
        void doPaintPixel(Graphics g, int x, int y, int scale);
    }

    public void paint(Graphics g, int scale, Palette palette) {
        doPaint(g, scale, palette, this::doPaintPixel);
    }

    public void paintInterlaced(Graphics g, int scale, Palette palette) {
        doPaint(g, scale, palette, this::doPaintPixelInterlaced);
    }

    private void doPaint(Graphics g, int scale, Palette palette, PaintPixel paintPixel) {
        for (int ya = 0; ya < 24; ya++)
            for (int xa = 0; xa < 32; xa++) {
                int attr = attrbuf[xa][ya];
                int index1 = attr & 7;
                int index2 = (attr >> 3) & 7;
                Color i1 = palette.getInkColor(index1, 0);
                Color i2 = palette.getInkColor(index1, 1);
                Color p1 = palette.getPaperColor(index2, 0);
                Color p2 = palette.getPaperColor(index2, 1);
                for (int yp = 0; yp < 8; yp++)
                    for (int xp = 0; xp < 8; xp++) {
                        int y = ya * 8 + yp;
                        int x = xa * 8 + xp;
                        switch (pixbuf[x][y]) {
                            case 0:
                                g.setColor(p1);
                                break;
                            case 1:
                                g.setColor(p2);
                                break;
                            case 2:
                                g.setColor(i1);
                                break;
                            default:
                                g.setColor(i2);
                                break;
                        }
                        paintPixel.doPaintPixel(g, x, y, scale);
                    }
            }
    }

    protected void doPaintPixelInterlaced(Graphics g, int x, int y, int scale) {
        g.drawLine(x * 2, y * 2, x * 2 + 1, y * 2);
        Color c = g.getColor();
        g.setColor(c.darker());
        g.drawLine(x * 2, y * 2 + 1, x * 2 + 1, y * 2 + 1);
    }

    protected void doPaintPixel(Graphics g, int x, int y, int scale) {
        int xx = x * scale;
        int yy = y * scale;
        g.fillRect(xx, yy, scale, scale);
        if (scale > 7) {
            g.setColor(Color.PINK);
            if (x % 8 == 0) g.drawLine(xx, yy, xx, yy + scale - 1);
            if (y % 8 == 0) g.drawLine(xx, yy, xx + scale - 1, yy);
        }
    }

    public int getPixel(int x, int y) {
        return (x >= 0 && x < 256 && y >= 0 && y < 192) ? pixbuf[x][y] : -1;
    }

    private void putPixel(int x, int y, byte pixel, byte attr) {
        this.pixbuf[x][y] = pixel;
        this.attrbuf[x / 8][y / 8] = attr;
    }

    public void setPixel(int x, int y, Palette.Table table, int index, byte shift) {
        int xx = x / 8;
        int yy = y / 8;
        byte a = attrbuf[xx][yy];
        byte s;
        if (table == Palette.Table.INK) {
            if (index>=0) a = (byte) ((a & 0x38) | index);
            s = 2;
        } else {
            if (index>=0) a = (byte) ((a & 7) | (index << 3));
            s = 0;
        }
        s = (byte) (shift | s);

        undo.add(x, y, pixbuf[x][y], attrbuf[xx][yy], s, a);
        attrbuf[xx][yy] = a;

        pixbuf[x][y] = s;
    }

    public void beginDraw() {
        undo.start();
    }

    public void endDraw() {
        undo.commit();
    }

    public void undoDraw() {
        Vector<UndoElement> elements = undo.undo();
        if (null != elements) {
            ListIterator<UndoElement> i = elements.listIterator(elements.size());
            while (i.hasPrevious()) {
                UndoElement e = i.previous();
                putPixel(e.x, e.y, e.pixel, e.attr);
            }
        }
    }

    public void redoDraw() {
        Vector<UndoElement> elements = undo.redo();
        if (null != elements) {
            ListIterator<UndoElement> i = elements.listIterator();
            while (i.hasNext()) {
                UndoElement e = i.next();
                putPixel(e.x, e.y, e.newPixel, e.newAttr);
            }
        }
    }

    public void drawLine(int ox, int oy, int nx, int ny, Palette.Table table, int index, byte shift) {
        float dx = nx - ox, dy = ny - oy;
        if (Math.abs(dx) < Math.abs(dy)) {
            dx = dx / Math.abs(dy);
            dy = Math.signum(dy);
        } else {
            dy = dy / Math.abs(dx);
            dx = Math.signum(dx);
        }
        float x = ox, y = oy;
        //drawPixel((int) x, (int) y, table, index, shift);
        while (Math.round(x) != nx || Math.round(y) != ny) {
            x += dx;
            y += dy;
            setPixel((int) x, (int) y, table, index, shift);
        }
    }

    public void fill(int x, int y, Palette.Table table, int index, byte shift) {
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(x, y));
        int pix = getPixel(x, y);
        int attr = getAttr(x, y);
        int npix = (table == Palette.Table.INK) ? (2 | shift) : shift;
        while (!stack.empty()) {
            Point p = stack.pop();
            int pixel = getPixel(p.x, p.y);
            if (pixel == npix || pixel != pix) continue;
            setPixel(p.x, p.y, table, index, shift);
            if (p.x > 0) stack.push(new Point(p.x - 1, p.y));
            if (p.y > 0) stack.push(new Point(p.x, p.y - 1));
            if (p.x < 255) stack.push(new Point(p.x + 1, p.y));
            if (p.y < 191) stack.push(new Point(p.x, p.y + 1));
        }
    }

    public int getAttr(int x, int y) {
        if (pixbuf[x][y] < 2)
            return (attrbuf[x / 8][y / 8] >> 3) & 7;
        else
            return attrbuf[x / 8][y / 8] & 7;
    }

    public void save(OutputStream stream) throws IOException {
        for (int i = 0; i < 256; i++)
            stream.write(pixbuf[i]);
        for (int i = 0; i < 32; i++)
            stream.write(attrbuf[i]);
    }

    public void load(InputStream stream) throws IOException {
        for (int i = 0; i < 256; i++) {
            int v = stream.read(pixbuf[i]);
            if (v != 192) stream.read(pixbuf[i], v, 192 - v);
            //System.out.println(i+" line, read "+v);
        }
        ;
        for (int i = 0; i < 32; i++) {
            int v = stream.read(attrbuf[i]);
            if (v != 24) stream.read(pixbuf[i], v, 24 - v);
        }
    }

    public void importSCR(InputStream stream) throws IOException {
        byte[] pix = new byte[2048 * 3];
        byte[] attr = new byte[768];
        stream.read(pix);
        stream.read(attr);
        boolean bright = false;
        for (int y = 0; y < 24; y++)
            for (int r = 0; r < 8; r++)
                for (int x = 0; x < 32; x++) {
                    byte a = attr[x + 32 * y];
                    if (r == 0) {
                        attrbuf[x][y] = (byte) (a & 0x3f);
                    }
                    bright = (a & 0x40) != 0;
                    byte b = pix[x + 32 * (y % 8) + 256 * r + 2048 * (y / 8)];
                    for (int i = 7; i >= 0; i--) {
                        byte p = (byte) ((b & 1) * 2 + (bright ? 0 : 1));
                        if ((a & 7) == 0 && p == 3) p = 2;
                        if ((a & 0x38) == 0 && p == 1) p = 0;
                        pixbuf[x * 8 + i][y * 8 + r] = p;
                        b >>= 1;
                    }
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

            Pair(int first, int second,List<Pair> pairs) {
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
                    if (p.first == -2 || p. second == -2) {
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
                        if (n.bestCount<bestCount) {
                            best = n.best;
                            bestCount = n.bestCount;
                        }
                    } else {
                        tries++;
                        if (tries>=maxTries) return best;
                    }
                }
                if (stat.size()<bestCount) {
                    bestCount = stat.size();
                    best = this;
                }
                stat.push(s);
                return null;
            }

            byte getAttr(Integer[] s) {
                int bestN = -1;
                byte best = 0;
                for (int i=0; i<ink.size(); i++) {
                    final Pair ip = ink.get(i);
                    for (int p=0; p<paper.size(); p++) {
                        final Pair pp = paper.get(p);
                        int n =0;
                        for (int k : s) {
                            if (k == ip.first || k == ip.second || k == pp.first || k == pp.second || k == -2)
                                n++;
                        }
                        final byte b = (byte) (i + (p << 3));
                        if (n == s.length) return b;
                        if (n>bestN) {
                            bestN = n;
                            best = b;
                        }
                    }
                }
                return best;
            }

            int[][] getInkPaper(){
                int[][] p = new int[2][8];
                Pair j;
                for (int i=0; i<8; i++) {
                    j = (i<ink.size()) ? ink.get(i) : new Pair(0,0);
                    p[0][i]=Palette.combine(j.first,j.second);
                    j = (i<paper.size()) ? paper.get(i) : new Pair(0,0);
                    p[1][i]=Palette.combine(j.first,j.second);
                }
                return p;
            }

            List<Integer> attrToList(byte attr) {
                List<Integer> l = new ArrayList<>();
                final Pair p = paper.get((attr>>3)&7);
                l.add(p.first);
                l.add(p.second);
                final Pair i = ink.get(attr & 7);
                l.add(i.first);
                l.add(i.second);
                return l;
            }
        }

        ColorConverter converter = Palette.createConverter();

        BufferedImage png = ImageIO.read(stream);
        if (png.getWidth() < 256 || png.getHeight() < 192) throw new IOException("Wrong png size");
        Integer c[][][] = new Integer[32][24][4];
        for (int x = 0; x < 32; x++)
            for (int y = 0; y < 24; y++) {
                Arrays.fill(c[x][y], -2);
                Map<Integer,Integer> cnt = new HashMap<>();
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        int i = converter.fromRGB(new Color(png.getRGB(x * 8 + xx, y * 8 + yy)));
                        cnt.merge(i, 1, (o,n) -> o+n);

                        /*for (int j = 0; j < 4; j++) {
                            if (c[x][y][j] == -2) c[x][y][j] = i;
                            if (c[x][y][j] == i) break;
                        }*/
                    }
                c[x][y] = Stream.concat(cnt.keySet().stream().sorted((f,g)-> cnt.get(g)-cnt.get(f)), Stream.of(-2,-2,-2,-2))
                        .limit(4).toArray(Integer[]::new);
                Arrays.sort(c[x][y]); //,(a,b)-> b-a);
            }

        Deque<Integer[]> stat = new LinkedList<>();
        for (int x = 0; x < 32; x++)
            for (int y = 0; y < 24; y++) {
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

        //if (pairs.size()>16) throw new IOException("Cannot import, " + pairs.size() + " pair of colors");

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


        for (int x = 0; x < 32; x++)
            for (int y = 0; y < 24; y++) {
                final byte attr = comb.getAttr(c[x][y]);
                attrbuf[x][y] = attr;
                List<Integer> l = comb.attrToList(attr);
                for (int xx = 0; xx < 8; xx++)
                    for (int yy = 0; yy < 8; yy++) {
                        final int color = converter.fromRGB(new Color(png.getRGB(x * 8 + xx, y * 8 + yy)));
                        final byte b = (byte) (l.indexOf(color));
                        pixbuf[x * 8 + xx][y * 8 + yy] = (b>=0) ? b : 0;
                    }
            }
        return comb.getInkPaper();
    }
}
