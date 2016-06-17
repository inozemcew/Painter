package Painter;

import Painter.Palette.Palette;

/**
 * Created by ainozemtsev on 17.06.16.
 */
public class PixelFormat {
    public final Palette.Table table;
    public final int index;
    public final int shift;

    public PixelFormat(Palette.Table table, int index, int shift) {
        this.index = index;
        this.shift = shift;
        this.table = table;
    }

    public PixelFormat(byte pixel, byte attr) {
        this((pixel < 2) ? Palette.Table.PAPER : Palette.Table.INK,
                (pixel < 2) ? Screen.paperFromAttr(attr) : Screen.inkFromAttr(attr),
                pixel & 1);

    }

    public int pack() {
        return (table == Palette.Table.INK) ? (2 | shift) : shift;
    }
}
