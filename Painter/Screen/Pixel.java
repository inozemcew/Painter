package Painter.Screen;

import Painter.Palette.Palette;

/**
 * Created by aleksey on 25.09.16.
 */
public class Pixel {
    public final Enum table;
    public final int index;
    public final int shift;

    public Pixel(Enum table, int index, int shift) {
        this.index = index;
        this.shift = shift;
        this.table = table;
    }

    public Pixel clone() {
        return new Pixel(this.table, this.index, this.shift);
    }

    public boolean equals(Pixel other) {
        return (this.table == other.table) && (this.index == other.index) && (this.shift == other.shift);
    }

    public boolean hasSameColor(Pixel other, Palette palette) {
        return palette.getRGBColor(table, index, shift).equals(palette.getRGBColor(other.table, other.index, other.shift));
    }

}
