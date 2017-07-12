package Painter.Screen.Mapper;

import Painter.Screen.ImageBuffer;

import java.awt.*;

/**
 * Created by ainozemtsev on 24.01.17.
 */
public class DefaultScreenMapper implements ScreenMapper {
    private final Dimension size, attrSize;
    protected Dimension tileFactor = new Dimension();
    protected Dimension pixelFactor = new Dimension();
    protected Dimension attrFactor = new Dimension();
    private int pixelToAttrRatio;

    public DefaultScreenMapper(int width, int height, Dimension pixelFactor, Dimension attrFactor) {
        this.pixelFactor.setSize(pixelFactor);
        this.attrFactor.setSize(attrFactor);
        this.size = new Dimension(width / pixelFactor.width, height / pixelFactor.height);
        this.attrSize = new Dimension(width / attrFactor.width, height / attrFactor.height);
        this.pixelToAttrRatio = attrFactor.width * attrFactor.height / pixelFactor.width / pixelFactor.height;
    }

    @Override
    public int getPixelBufferSize() {
        return size.width * size.height;
    }

    @Override
    public Dimension getSizes() {
        return this.size;
    }

    @Override
    public Dimension getAttrSizes() {
        return this.attrSize;
    }

    @Override
    public int getAttrBufferSize() {
        return attrSize.width * attrSize.height;
    }

    public Dimension getAttrFactor() {
        return attrFactor;
    }

    /*    protected ImageBuffer createImageBuffer(int width, int height) {
                final int size = width * height / pixelFactor.width / pixelFactor.height;
                final int aSize = width * height / attrFactor.height / attrFactor.width;
                return new ImageBuffer(size, aSize);
            }
        */
    @Override
    public int pixelOffset(int x, int y) {
        return (x + size.width * y );
    }

    @Override
    public int attrOffset(int x, int y) {
        return (x + attrSize.width * y);
    }

    @Override
    public int attrOffsetFromPixelOffset(int offset) {
        return offset / pixelToAttrRatio;
    }

    public int alignX(int x) {
        return x / tileFactor.width * tileFactor.width;
    }

    public int alignY(int y) {
        return y / tileFactor.height * tileFactor.height;
    }


}
