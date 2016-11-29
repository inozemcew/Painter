package NPainter.Convert;

import Painter.Screen.Palette.Palette;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ainozemtsev on 21.11.16.
 */

class ComplementMaps extends HashMap<List<Integer>, Map<ColorCell,ColorCell>> { }

class Combinator {
    private Set<ColorCell> ink, paper;
    private static int tries = 0;
    private static final int maxTries = 555000;
    private static int bestCount;
    private static Combinator best;

    private ComplementMaps complementMaps;

    private final int SIZE  = 8; //TODO: rework!

    static Combinator createCombinator(ColorMatrix colors4Tiles) {
        Deque<List<Integer>> stat = Combinator.getStat(colors4Tiles);

        List<ColorCell> colorCells = Combinator.getAllPairs(stat);

        ComplementMaps complementMaps = Combinator.createComplementMaps(stat, colorCells);
        tries = 0;
        bestCount = Integer.MAX_VALUE;
        best = new Combinator(new HashSet<>(8), new HashSet<>(8), complementMaps);
        Combinator comb = best.next(stat);
        return (comb != null) ? comb : best;
    }

    private Combinator(Set<ColorCell> ink, Set<ColorCell> paper, ComplementMaps complementMaps) {
        this.ink = new HashSet<>(ink);
        this.paper = new HashSet<>(paper);
        this.complementMaps = complementMaps;
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

    static List<ColorCell> getAllPairs(Deque<List<Integer>> stat) {
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
        return allPairs;
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

    enum Match { All, OneHalf, One, None}

    static Match rank(ColorCell cell, ColorCell complement,
                      Collection<ColorCell> ink, Collection<ColorCell> paper) {
        final boolean inkContains = ink.stream().anyMatch(c -> c.contains(cell));
        final boolean paperContains = paper.stream().anyMatch(c -> c.contains(complement));
        if (inkContains && paperContains) return Match.All;

        final boolean b1 = cell.getSize() == 1;
        final boolean containsInk = ink.stream().anyMatch(c -> cell.contains(c) || (c.getSize() == 1 && b1));
        final boolean b2 = complement.getSize() == 1;
        final boolean containsPaper = paper.stream().anyMatch(c -> complement.contains(c) || (c.getSize() == 1 && b2));
        if ((inkContains||containsInk) && (paperContains||containsPaper)) return Match.OneHalf;

        if (inkContains || paperContains) return Match.One;
        return Match.None;
    }

    int compare(ColorCell x, ColorCell y, Map<ColorCell, ColorCell> complements) {
        return (y.getCount() + complements.get(y).getCount()) - (x.getCount() + complements.get(x).getCount());
    }


    List<ColorCell> sortPairs(Map<ColorCell, ColorCell> map) {
        Map<Match, List<ColorCell>> m = map.keySet().stream()
                .collect(Collectors.groupingBy(p -> rank(p, map.get(p), ink, paper)));
        if (m.containsKey(Match.All))
            return m.get(Match.All);
        return Stream.concat(Stream.concat(
                    m.getOrDefault(Match.One, new ArrayList<>()).stream()
                            .sorted((x, y) -> compare(x, y, map)),
                    m.getOrDefault(Match.OneHalf, new ArrayList<>()).stream()
                            .sorted((x, y) -> compare(x, y, map))),
                m.getOrDefault(Match.None, new ArrayList<>()).stream()
                        .sorted((x, y) -> compare(x, y, map))
            ).collect(Collectors.toList());
    }

    static void addTo(Set<ColorCell> set, ColorCell p) {
        if (p.isEmpty()) return;
        if (!set.contains(p)) {
            if (set.stream().anyMatch(i -> i.contains(p))) return;
            set.removeIf(p::contains);
            if (p.getSize()<2) {
                Optional<ColorCell> p2 = set.stream().filter(c -> c.getSize() < 2).findAny();
                if (p2.isPresent()) {
                    ColorCell p3 = new ColorCell(p2.get());
                    p3.merge(p);
                    set.remove(p2.get());
                    set.add(p3);
                    return;
                }
            }
            set.add(p);
        }
    }

    Combinator next(Deque<List<Integer>> uniqueColorsStack) {
        if (uniqueColorsStack.isEmpty()) return this;
        final List<Integer> s = uniqueColorsStack.pop();
        final Map<ColorCell, ColorCell> map = complementMaps.get(s);
        for (ColorCell p : sortPairs(map) ) {
            Combinator n = new Combinator(this.ink, this.paper, this.complementMaps);
            addTo(n.ink, p);
            addTo(n.paper, map.get(p));
            if (n.ink.size() <= SIZE && n.paper.size() <= SIZE) {
                Combinator r = n.next(uniqueColorsStack);
                if (r != null) return r;
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
