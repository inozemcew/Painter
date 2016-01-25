package Painter;

import Painter.Palette.Palette;

import java.awt.*;

/**
 * Created by ainozemtsev on 21.01.16.
 */
public interface ImageSupplier {

    interface ImageChangeListener extends Palette.PaletteChangeListener {
        void imageChanged(int x, int y, int w, int h);
        void imageChanged();
    }

    int getImageWidth();
    int getImageHeight();
    Color getPixelColor(int x, int y);
    void  addChangeListener(ImageChangeListener listener);
    //void ScrollInView(int x, int y);
}
