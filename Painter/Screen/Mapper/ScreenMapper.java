package Painter.Screen.Mapper;

import java.awt.*;

/**
 * Created by ainozemtsev on 11.07.17.
 */
public interface ScreenMapper {
    Dimension getSizes();
    Dimension getAttrSizes();
    int getPixelBufferSize();
    int getAttrBufferSize();
    int pixelOffset(int x, int y);
    int attrOffset(int x, int y);
    int attrOffsetFromPixelOffset(int offset);
}
