package Painter.Screen;

import Painter.Palette.Palette;

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

    public Pixel clone() {
        return new Pixel(this.table, this.index, this.shift);
    }

    public boolean equals(Pixel other) {
        return (this.table == other.table) && (this.index == other.index) && (this.shift == other.shift);
    }

    public boolean hasSameColor(Pixel other, PixelProcessing processor, Palette palette) {
        return processor.getPixelColor(this,palette).equals(processor.getPixelColor(other,palette));
    }

}
