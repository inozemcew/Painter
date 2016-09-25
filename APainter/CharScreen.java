package APainter;


import Painter.Screen.Pixel;

import java.awt.*;

/**
 * Created by ainozemtsev on 08.07.16.
 */
public class CharScreen extends AScreen {
    private static final int SIZE_X = 48;
    private static final int SIZE_Y = 256;
    private byte attr;

    public CharScreen() {
        super(SIZE_X, SIZE_Y);
        final byte attr = 7;
        final byte bAttr = 1;
        final byte bChr = 63;
        byte i = 0;
        Point p = new Point();
        for (int y = 0; y < SIZE_Y; y += pixelFactor.height * 2) {
            for (int x = 0; x < SIZE_X; x += pixelFactor.width * 2) {
                p.setLocation(x, y);
                putPixelData(p, i++, attr);
                p.setLocation(x + pixelFactor.width, y);
                putPixelData(p, bChr, bAttr);
                p.setLocation(x, y + pixelFactor.height);
                putPixelData(p, bChr, bAttr);
                p.setLocation(x + pixelFactor.width, y + pixelFactor.height);
                putPixelData(p, bChr, bAttr);
            }
        }
    }

    @Override
    protected byte getAttr(Point pos) {
        return this.attr;
    }

    @Override
    public Pixel getPixel(Point pos) {
        Pixel pixel = super.getPixel(pos);
        return new Pixel(pixel.table,super.getAttr(pos),pixel.shift);
    }

    public void setAttr(int index) {
        final byte oldAttr = 0;
        this.attr = pixelProcessor.packAttr(new Pixel(Table.Fore, index, 0), oldAttr, new Point());
    }
}
