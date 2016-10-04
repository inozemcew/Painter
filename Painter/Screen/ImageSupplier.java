package Painter.Screen;

import Painter.Screen.Palette.Palette;

import java.awt.*;

/**
 * Created by ainozemtsev on 21.01.16.
 */
public interface ImageSupplier {

    interface ImageChangeListener extends Palette.PaletteChangeListener {
        void imageChanged(int x, int y, int w, int h);
        void imageChanged();
    }

    enum Status {
        Normal, Dimmed, Enhanced
    }

    int getImageWidth();
    int getImageHeight();
    Color getPixelColor(Point pos);
    void  addChangeListener(ImageChangeListener listener);
    //void ScrollInView(int x, int y);
    default Status getStatus(Point pos) {
        return Status.Normal;
    }
}
