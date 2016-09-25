package NPainter;

import Painter.Palette.Palette;
import Painter.Screen.Pixel;
import Painter.Screen.PixelProcessing;

import java.awt.*;
import java.awt.print.Paper;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by aleksey on 25.09.16.
 */
public enum  PixelProcessor implements PixelProcessing {
    MODE4("4 colors mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            return (byte) (pixel.shift | ((pixel.table == NScreen.Table.INK) ? 2 : 0) );
        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            NScreen.Table table = (pixelData < 2) ? NScreen.Table.PAPER : NScreen.Table.INK;
            int index = (pixelData < 2) ? paperFromAttr(attrData) : inkFromAttr(attrData);
            return new Pixel(table, index, pixelData & 1);
        }

        @Override
        public Color getPixelColor(Pixel pixel, Point pos, Palette palette) {
            return palette.getRGBColor(pixel.table, pixel.index, pixel.shift);
        }

    };

    private String name;

    PixelProcessor(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return name;
    }

    @Override
    public Set<? extends PixelProcessing> enumPixelModes() {
        return EnumSet.allOf(PixelProcessor.class);
    }

    @Override
    public byte packAttr(Pixel pixel, byte oldAttrData, Point pos) {
        if (pixel.table == NScreen.Table.INK)
            return inkToAttr(oldAttrData,pixel.index);
        else
            return paperToAttr(oldAttrData,pixel.index);
    }

    public static byte paperToAttr(byte attr, int paper) { return (byte) ((attr & 7) | (paper<<3));}

    public static byte inkToAttr(byte attr, int ink) { return (byte) ((attr & 0x38) | ink);}

    public static int paperFromAttr(byte attr) { return (attr >> 3) & 7; }

    public static int inkFromAttr(byte attr) { return attr & 7; }

    public static int fromAttr(byte attr, NScreen.Table table) {
        return (table == NScreen.Table.INK) ? inkFromAttr(attr) : paperFromAttr(attr);
    }

    public static byte toAttr(byte attr, int value, NScreen.Table table) {
        return (table == NScreen.Table.INK) ? inkToAttr(attr,value) : paperToAttr(attr,value);
    }
}
