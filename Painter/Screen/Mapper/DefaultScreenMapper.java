package Painter.Screen.Mapper;

import Painter.Screen.ImageBuffer;

import java.awt.*;

/**
 * Created by ainozemtsev on 24.01.17.
 */
public class DefaultScreenMapper {
    protected ImageBuffer image;
    protected Dimension tileFactor = new Dimension();
    protected Dimension pixelFactor = new Dimension();
    protected Dimension attrFactor = new Dimension();

    public DefaultScreenMapper(int width, int height) {
        initFactors();
        this.image = createImageBuffer(width, height);
    }

    protected void initFactors() {
        tileFactor.setSize(8, 8);
        pixelFactor.setSize(2, 1);
        attrFactor.setSize(8, 8);
    }

    protected ImageBuffer createImageBuffer(int width, int height) {
        return new ImageBuffer(width, height, pixelFactor, attrFactor);
    }

    protected byte getPixelData(Point pos) {
        final int xx = pos.x / pixelFactor.width;
        final int yy = pos.y / pixelFactor.height;
        return image.getPixel(xx,yy);
    }

    protected byte getAttr(Point pos) {
        final int xx = pos.x / pixelFactor.width;
        final int yy = pos.y / pixelFactor.height;
        return image.getAttr(xx, yy);
    }

    private void putAttr(Point pos, byte attr) {
        final int xx = pos.x / pixelFactor.width;
        final int yy = pos.y / pixelFactor.height;
        image.putAttr(xx, yy, attr);

    }

    protected void putPixelData(Point pos, byte pixel, byte attr) {
        final int xx = pos.x / pixelFactor.width;
        final int yy = pos.y / pixelFactor.height;
        image.putPixel(xx, yy, pixel, attr);
    }

    protected void putPixelData(Point pos, byte pixel) {
        final int xx = pos.x / pixelFactor.width;
        final int yy = pos.y / pixelFactor.height;
        image.putPixel(xx, yy, pixel);
    }

    public int alignX(int x) {
        return x / tileFactor.width * tileFactor.width;
    }

    public int alignY(int y) {
        return y / tileFactor.height * tileFactor.height;
    }


}
