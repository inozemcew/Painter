package NPainter;

import Painter.Screen.Palette.Palette;
import Painter.Screen.Pixel;
import Painter.Screen.PixelProcessing;

import java.awt.*;
import java.util.AbstractList;
import java.util.EnumSet;
import java.util.Set;

import static NPainter.NScreen.Table;

/**
 * Created by aleksey on 25.09.16.
 */
public enum  PixelProcessor implements PixelProcessing {
    MODE4("4 colors mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            final int b = super.packPixel(pixel, oldPixelData, pos);
            return replace(oldPixelData, b, pos.x & 1);
        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            int b = split(pixelData, pos.x & 1);
            Table table = (b < 2) ? Table.PAPER : Table.INK;
            int index = (b < 2) ? paperFromAttr(attrData) : inkFromAttr(attrData);
            return new Pixel(table, index, b & 1);
        }
    },

    MODE6("6 colors mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            Point s_pos = new Point(pos.x ^ 1, pos.y);
            Pixel s_pix = unpackPixel(oldPixelData, s_pos);
            if (pixel.shift > 1)
                return (pixel.shift > 2) ? combine(1, 3) : combine(3, 1);
            if (s_pix.shift > 1) s_pix = pixel;
            if (pixel.shift > 0 && pixel.table != s_pix.table) {
                s_pix = new Pixel(s_pix.table, s_pix.index, 0);
            }
            return MODE4.packPixel(pixel, MODE4.packPixel(s_pix, (byte) 0, s_pos), pos);

        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            int pix1 = split(pixelData, 0), pix2 = split(pixelData, 1);
            if ((pix1 ^ pix2) == 2 && (pix1 & pix2) == 1)
                return new Pixel(Table.PAPER, paperFromAttr(attrData), (pix1 & 2) == 2 ? 2 : 3);
            return MODE4.unpackPixel(pixelData, attrData, pos);
        }
    },

    MODE8("8 colors mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            if (pixel.shift > 1) {
                if (pixel.table == Table.INK)
                    return (pixel.shift > 2) ? combine(1, 2) : combine(2, 1);
                return (pixel.shift > 2) ? combine(1, 3) : combine(3, 1);
            }
            int pix1 = split(oldPixelData,0), pix2 = split(oldPixelData,1);
            if ((pix1 == 1 && pix2 > 1) || (pix1 >1 && pix2 == 1)) {
                int b = super.packPixel(pixel, oldPixelData, pos);
                return combine(b,b);
            }
            return MODE6.packPixel(pixel, oldPixelData, pos);
        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            int pix1 = split(pixelData, 0), pix2 = split(pixelData, 1);
            if ((pix1 ^ pix2) == 3 && (pix1 * pix2) != 0 )
                return new Pixel(Table.INK, inkFromAttr(attrData), (pix1 & 2) == 2 ? 2 : 3);
            return MODE6.unpackPixel(pixelData, attrData, pos);
        }
    },

    MODE12("1+2 bits mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            int t = (pixel.table == Table.INK) ? 2:0;
            int p1 = pixel.shift & 1;
            int p2 = split(oldPixelData,(pos.x ^ 1) & 1 ) & 1;
            if ((pos.x & 1) == 0 ) {
                p1 |= (pixel.shift & 2) ;
                p2 |= t;
                return combine(p1,p2);
            } else {
                p2 |= (pixel.shift & 2) ;
                p1 |= t;
                return combine(p2,p1);
            }
        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            final int p = split(pixelData, pos.x & 1) & 1;
            int shift = (split(pixelData, 0) & 2)  |  p;
            Table table = ((split(pixelData, 1) & 2)== 0) ? Table.PAPER : Table.INK;
            int index = fromAttr(attrData, table);
            return new Pixel(table, index, shift);
        }
    },

    MODE5("5+1 colors mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            Point s_pos = new Point(pos.x ^ 1, pos.y );
            Pixel sibling = unpackPixel(oldPixelData, s_pos);
            if ((pixel.table != Table.PAPER || pixel.shift != 0)
                    && (sibling.table != Table.PAPER || sibling.shift != 0)) {
                sibling = pixel;
            }
            if (sibling.shift > 1) {
                if (pixel.shift > 1) return (pixel.shift > 2) ? combine(2, 3) : combine(3, 2);
                return ((pos.x & 1) == 0) ? combine(1, sibling.shift) : combine(sibling.shift, 1);
            }
            if (pixel.shift > 1) return ((pos.x & 1) == 0) ? combine(pixel.shift, 1) : combine(1, pixel.shift);
            return MODE4.packPixel(pixel,MODE4.packPixel(sibling,(byte)0,s_pos),pos);
        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            int pix = split(pixelData, pos.x & 1);
            int pix1 = split(pixelData, 0), pix2 = split(pixelData, 1);
            if ((pix1 ^ pix2) == 1 && (pix1 & pix2) > 1) // Ink-Ink -> width = 2
                return new Pixel(Table.PAPER, paperFromAttr(attrData), 2 | (pix2 & 1));
            if ((pix1==1 || pix2==1) && (pix1>1 || pix2>1)) { // Ink-Paper -> width = 1
                if (pix > 1) return new Pixel(Table.PAPER, paperFromAttr(attrData), 2 | (pix & 1));
                else return new Pixel(Table.PAPER, paperFromAttr(attrData), 0);
            }
            return MODE4.unpackPixel(pixelData, attrData, pos);
        }
    },

    MODEX("X mode") {
        @Override
        public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
            Point s_pos = new Point(pos.x ^ 1, pos.y );
            Pixel sibling = unpackPixel(oldPixelData, s_pos);
            if (pixel.table == Table.PAPER && pixel.shift == 2) {
                if (sibling.table == Table.PAPER && sibling.shift == 0)
                    return ((pos.x & 1) == 0) ? combine(1, 2) : combine(2, 1);
                return combine(3,1);
            }
            if (sibling.table == Table.PAPER && sibling.shift == 2) {
                if (pixel.table == Table.PAPER && pixel.shift == 0)
                    return ((pos.x & 1) == 1) ? combine(1,2): combine(2,1);
            }
            if ((pixel.table == Table.INK && pixel.shift < 2 && sibling.table == Table.PAPER && sibling.shift > 0)
                ||(pixel.table == Table.PAPER && pixel.shift > 0 && sibling.table == Table.INK && sibling.shift < 2)) {
                sibling = pixel;
            }

            return MODE6.packPixel(pixel, MODE6.packPixel(sibling, (byte)0, s_pos), pos);
        }

        @Override
        public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
            int pix = split(pixelData, pos.x & 1);
            int pix1 = split(pixelData, 0), pix2 = split(pixelData, 1);
//            if ((pix1 ^ pix2) == 1 && (pix1 & pix2) > 1) // Ink-Ink -> width = 2
//                return new Pixel(Table.PAPER, paperFromAttr(attrData), 2 | (pix2 & 1));
            if ((pix1==1 || pix2==1) && (pix1==2 || pix2==2)) { // Ink-Paper -> width = 1
                if (pix < 2) return new Pixel(Table.PAPER, paperFromAttr(attrData), 2);
                else return new Pixel(Table.PAPER, paperFromAttr(attrData), 0);
            }
            return MODE6.unpackPixel(pixelData, attrData, pos);
        }
    }

    ;

    protected Pixel unpackPixel(byte oldPixelData, Point pos) {
        return unpackPixel(oldPixelData, (byte) 0, pos);
    }

    public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
        return (byte) ((pixel.shift & 1)  | ((pixel.table == Table.INK) ? 2 : 0));
    }

    private String name;

    PixelProcessor(String name) {
        this.name = name;
    }

    @Override
    public Color getPixelColor(Pixel pixel, Palette palette) {
        final int shift = pixel.shift;
        if (shift<2)
            return palette.getRGBColor(pixel.table, pixel.index, shift);
        NScreen.Table table = (pixel.table == NScreen.Table.INK) ? NScreen.Table.PAPER : NScreen.Table.INK;
        return palette.getRGBColor(table, pixel.index, shift-2);
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

    public static byte packAttr(int ink, int paper) {
        return  (byte) (ink | paper <<3);
    }


    public static class PixelDataList extends AbstractList<Integer> {
        private final int SIZE = 2;
        byte pixelData;

        public PixelDataList() {
            this((byte)0);
        }

        public PixelDataList(byte pixelData) {
            this.pixelData = pixelData;
        }

        public void setPixelData(byte pixelData) {
            this.pixelData = pixelData;
        }

        public byte getPixelData() {
            return pixelData;
        }

        @Override
        public Integer get(int index) {
            return split(pixelData, index);
        }

        @Override
        public Integer set(int index, Integer element) {
            Integer result = get(index);
            pixelData = replace(pixelData, element, index);
            return result;
        }

        @Override
        public int size() {
            return SIZE;
        }
    }

    public static int split(byte b, int x) {
        return (b>>(4*x)) & 0x0f;
    }

    public static byte replace(byte pixelData, int pix, int x) {
        final int x4 = x * 4;
        return (byte) ( (pixelData & (~(3 << x4))) | ((pix & 3) << x4) );
    }

    public static byte combine(int... b ) {
        int result = 0;
        for (int i = 0; i < b.length; i++) result |= b[i] << (i * 4);
        return (byte) result;
    }
}

