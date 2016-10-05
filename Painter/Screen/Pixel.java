package Painter.Screen;

import Painter.Screen.Palette.Palette;

/**
 * Created by aleksey on 25.09.16.
 */
public class Pixel {
    public Enum table;
    public int index;
    public int shift;

    public Pixel(Enum table, int index, int shift) {
        this.index = index;
        this.shift = shift;
        this.table = table;
    }

    public Pixel(Pixel other) {
        this(other.table, other.index, other.shift);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Pixel)) return false;
        Pixel o = (Pixel)other;
        return (this.table == o.table) && (this.index == o.index) && (this.shift == o.shift);
    }

    @Override
    public int hashCode() {
        return table.hashCode() << 16 + index << 4 + shift;
    }

    public boolean hasSameColor(Pixel other, PixelProcessing processor, Palette palette) {
        return processor.getPixelColor(this,palette).equals(processor.getPixelColor(other,palette));
    }

}
