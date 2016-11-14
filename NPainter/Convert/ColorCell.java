package NPainter.Convert;

import java.util.*;

/**
 * Created by ainozemtsev on 09.11.16.
 */
public class ColorCell {
    private Set<Integer> colors = new HashSet<>();
    private int count;

    public ColorCell(Integer... colors) {
        this.colors.addAll(Arrays.asList(colors));
        this.count = 0;
    }

    public ColorCell(Collection<ColorCell> colorCells, Integer... colors) {
        this(colors);
        if (colorCells == null) return;
        this.count = colorCells.stream().filter(this::equals).mapToInt(p -> p.count).findFirst().orElse(0);
    }

    public ColorCell(ColorCell that) {
        this.colors = new HashSet<>(that.colors);
        this.count = that.getCount();
    }

    void inc() {
        count++;
    }

    public int getCount() {
        return count;
    }

    boolean isEmpty() {
        return colors.isEmpty();
    }

    boolean contains(ColorCell that) {
        return colors.containsAll(that.colors);
    }

    boolean equals(Integer... others) {
        if (others.length > colors.size())
            return false;
        for( Integer other:others) {
            if (!colors.contains(other))
                return false;
        }
        return true;
    }

    boolean equals(ColorCell other) {
        return (this.colors.equals(other.colors));
    }

    @Override
    public int hashCode() {
        return this.colors.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ColorCell)) return false;
        ColorCell o = (ColorCell) obj;
        return this.equals(o);
    }

    static ColorCell createAndAdd(List<ColorCell> colorCells, Integer... colors) {
        ColorCell p = new ColorCell(colorCells, colors);
        colorCells.add(p);
        return p;
    }

    ColorCell complement(List<Integer> s) {
        return new ColorCell(s.stream().filter(c -> !colors.contains(c)).toArray(Integer[]::new));
    }

    int rank(List<Integer> s, Collection<ColorCell> ink, Collection<ColorCell> paper) {
        final ColorCell complement = this.complement(s);
        final boolean inkContains = ink.stream().anyMatch(c-> c.contains(this));
        final boolean paperContains = paper.stream().anyMatch(c -> c.contains(complement));
        if (inkContains && paperContains) return 0;
        if (inkContains || paperContains) return 1;
        return 2;
    }

    int compareTo(ColorCell y, List<Integer> s) {
        return (y.count + y.complement(s).count) - (this.count + this.complement(s).count);
    }

    int[] asArray() {
        return colors.stream().mapToInt(i -> i).toArray();
    }
}
