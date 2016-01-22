package Painter;

import Painter.Palette.Palette;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by aleksey on 22.01.16.
 */
public class Screen implements ImageSupplier {
    private ImageBuffer image = new ImageBuffer();
    private Palette palette = new Palette();
    private Collection<ImageChangeListener> listeners = new ArrayList<>();

    @Override
    public void addChangeListener(ImageChangeListener listener) {
        this.listeners.add(listener);
        palette.addChangeListener(listener);
    }

    @Override
    public Color getPixelColor(int x, int y) {
        int attr = image.getAttr(x, y);
        int pix = image.getPixel(x, y);
        if (pix < 2)
            return palette.getPaperColor((attr >> 3) & 7, pix & 1);
        else
            return palette.getInkColor(attr & 7, pix & 1);
    }

    @Override
    public int getImageHeight() {
        return ImageBuffer.SIZE_Y;
    }

    @Override
    public int getImageWidth() {
        return ImageBuffer.SIZE_X;
    }

    Palette getPalette() {
        return palette;
    }

    ImageBuffer getImage() {
        return image;
    }

    String getPixelDescription(int x, int y) {
        int v = image.getPixel(x, y);
        byte attr = image.getAttr(x, y);
        return ((v < 2) ? "Paper" : "Ink") + String.valueOf(v & 1)
                + "=" + String.valueOf(((v < 2) ? attr : attr >> 3) & 7);
    }

    private boolean isInImage(Point p) {
        return isInImage(p.x, p.y);
    }

    boolean isInImage(int x, int y) {
        return x >= 0 && x < ImageBuffer.SIZE_X && y >= 0 && y < ImageBuffer.SIZE_Y;
    }

    void setPixel(int x, int y, Palette.Table table, int index, byte shift) {
        if (isInImage(x, y)) {
            image.setPixel(x, y, table, index, shift);
            listeners.forEach(l -> l.imageChanged(x / 8 * 8, y / 8 * 8, 8, 8));
        }
    }

    void drawLine(int ox, int oy, int x, int y, Palette.Table table, int index, byte shift) {
        if (isInImage(x, y) && isInImage(ox, oy)) {
            image.drawLine(ox, oy, x, y, table, index, shift);
            int dx, dy, w, h;
            if (ox < x) {
                dx = ox / 8 * 8;
                w = x / 8 * 8 + 8 - dx;
            } else {
                dx = x / 8 * 8;
                w = ox / 8 * 8 + 8 - dx;
            }
            if (oy < y) {
                dy = oy / 8 * 8;
                h = y / 8 * 8 + 8 - dy;
            } else {
                dy = y / 8 * 8;
                h = oy / 8 * 8 + 8 - dy;
            }
            listeners.forEach(l -> l.imageChanged(dx, dy, w, h));
        }
    }

    void fill(int x, int y, Palette.Table table, int index, byte shift) {
        if (isInImage(x, y)) {
            image.beginDraw();
            image.fill(x, y, table, index, shift);
            image.endDraw();
            listeners.forEach(ImageChangeListener::imageChanged);
        }
    }


}
