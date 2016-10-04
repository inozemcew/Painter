package Painter.Screen;

import Painter.Screen.Palette.Palette;

import java.awt.*;
import java.util.Set;

/**
 * Created by aleksey on 25.09.16.
 */
public interface PixelProcessing {
    String toString();
    Set<? extends PixelProcessing> enumPixelModes();
    byte packPixel(Pixel pixel, byte oldPixelData, Point pos);
    byte packAttr(Pixel pixel, byte oldAttrData, Point pos);
    Pixel unpackPixel(byte pixelData, byte attrData, Point pos);
    Color getPixelColor(Pixel pixel, Palette palette);
}
