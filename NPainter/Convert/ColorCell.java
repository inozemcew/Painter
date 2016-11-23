package NPainter.Convert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ainozemtsev on 09.11.16.
 */
class ColorCell {
    private Set<Integer> colors = new HashSet<>();
    private int count;
    private int hashCode;

    ColorCell(Integer... colors) {
        this.colors.addAll(Arrays.asList(colors));
        this.hashCode = this.colors.hashCode();
        this.count = 0;
    }

    ColorCell(Collection<ColorCell> colorCells, Integer... colors) {
        this(colors);
        if (colorCells == null) return;
        this.count = colorCells.stream().filter(this::equals).mapToInt(p -> p.count).findFirst().orElse(0);
    }

    ColorCell(ColorCell that) {
        this.colors = new HashSet<>(that.colors);
        this.count = that.getCount();
    }

    void inc() {
        count++;
    }

    public int getCount() {
        return count;
    }

    int getSize() {
        return colors.size();
    }

    void merge(ColorCell that) {
        colors.addAll(that.colors);
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
        return this.hashCode;
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

    ColorCell complement(List<Integer> s, List<ColorCell> cells) {
        return new ColorCell(cells, s.stream().filter(c -> !colors.contains(c)).toArray(Integer[]::new));
    }

    int[] asArray() {
        return colors.stream().mapToInt(i -> i).toArray();
    }

    @Override
    public String toString() {
        return colors.stream().map(i -> String.valueOf(i) + ", ").collect(Collectors.joining()) + "count = " + count ;
    }
}
