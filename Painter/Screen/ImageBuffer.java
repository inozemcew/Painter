package Painter.Screen;

import Painter.Screen.Mapper.ScreenMapper;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ainozemtsev on 23.11.15.
 * Image buffer for 2 bitplanes
 */
public class ImageBuffer {
//    private final int SIZE_X, SIZE_Y;
    private final ScreenMapper screenMapper;
    //private final Dimension pixelFactor;
//    private final int ATTR_SIZE_X, ATTR_SIZE_Y;
//    private final int ATTR_FACTOR_X, ATTR_FACTOR_Y;
    private byte pixbuf[], attrbuf[];

    public ImageBuffer(ScreenMapper screenMapper) { //Dimension pixelFactor, Dimension attrFactor) {
        this.screenMapper = screenMapper;
        //this.pixelFactor = new Dimension(pixelFactor);
//        this.SIZE_X = sizeX / pixelFactor.width;
//        this.SIZE_Y = sizeY / pixelFactor.height;
//        this.ATTR_FACTOR_X = attrFactor.width / pixelFactor.width;
//        this.ATTR_FACTOR_Y = attrFactor.height / pixelFactor.height;
//        this.ATTR_SIZE_X = SIZE_X / ATTR_FACTOR_X;
//        this.ATTR_SIZE_Y = SIZE_Y / ATTR_FACTOR_Y;
        this.pixbuf = createPixbuf();
        this.attrbuf = new byte[screenMapper.getAttrBufferSize()];
    }

    protected byte[] createPixbuf() {
        return new byte[screenMapper.getPixelBufferSize()];
    }

    private UndoRedo undo = null;
    private class UndoPixelItem implements UndoRedo.Item {
        private final Point pos;
        private final byte pixel, newPixel;
        private final byte attr, newAttr;

        UndoPixelItem(int x, int y, byte pixel, byte attr,
                      byte newPixel, byte newAttr) {
            this.pos = new Point(x,y);
            this.pixel = pixel;
            this.attr = attr;
            this.newPixel = newPixel;
            this.newAttr = newAttr;
        }
        @Override
        public void undo() {
            putPixel(this.pos.x, this.pos.y, this.pixel, this.attr);
        }
        @Override
        public void redo() {
            putPixel(this.pos.x, this.pos.y, this.newPixel, this.newAttr);
        }
    }

    private class UndoAttrItem implements UndoRedo.Item {
        private final Point pos;
        private final byte attr, newAttr;

        UndoAttrItem(int x, int y, byte attr, byte newAttr) {
            this.pos = new Point(x, y);
            this.attr = attr;
            this.newAttr = newAttr;
        }

        @Override
        public void undo() {
            putAttr(this.pos.x, this.pos.y, this.attr);
        }
        @Override
        public void redo() {
            putAttr(this.pos.x, this.pos.y, this.newAttr);
        }
    }

    public void setUndo(UndoRedo undo) {
        this.undo = undo;
    }

    public byte getPixel(int x, int y) {
        int offset = screenMapper.pixelOffset(x, y);
        int SIZE = screenMapper.getPixelBufferSize();
        return (offset >= 0 && offset < SIZE) ? pixbuf[offset] : -1;
    }

    public byte getAttr(int x, int y) {
        return attrbuf[screenMapper.attrOffset(x, y)];
    }

    public void putAttr(int x, int y, byte attr) {
        int offset = screenMapper.attrOffset(x, y);
        byte oldAttr = attrbuf[offset];
        attrbuf[offset] = attr;
        if (undo != null) undo.add(new UndoAttrItem(x, y, oldAttr, attr));
    }

    public void putPixel(int x, int y, byte pixel) {
        byte attr = getAttr(x, y);
        int offset = screenMapper.pixelOffset(x,y);
        byte oldPixel = pixbuf[offset];
        this.pixbuf[offset] = pixel;
        if (undo != null) undo.add(new UndoPixelItem(x, y, oldPixel, attr, pixel, attr));
    }

    public void putPixel(int x, int y, byte pixel, byte attr) {
        int aOffset = screenMapper.attrOffset(x, y);
        byte oldAttr = attrbuf[aOffset];
        int offset = screenMapper.pixelOffset(x,y);
        byte oldPixel = pixbuf[offset];
        this.pixbuf[offset] = pixel;
        this.attrbuf[aOffset] = attr;
        if (undo != null) undo.add(new UndoPixelItem(x, y, oldPixel, oldAttr, pixel, attr));
    }

    void shift (int dx, int dy) {
        byte newPixBuf[] = createPixbuf();
        Dimension SIZE = screenMapper.getSizes();
        for (int x = 0; x < SIZE.width; x++) {
            for (int y = 0; y < SIZE.height; y++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < SIZE.width && ny >= 0 && ny <SIZE.height) {
                    newPixBuf[screenMapper.pixelOffset(nx, ny)] = pixbuf[screenMapper.pixelOffset(x,y)];
                }
            }
        }
        pixbuf = newPixBuf;
    }

    @FunctionalInterface
    public interface PixelDataProcessor {
        byte process(byte b, byte a, int offset);
    }

    @FunctionalInterface
    public interface AttrDataProcessor {
        byte process(byte a, int offset);
    }

    public void forEachPixel(PixelDataProcessor proc) {
        for (int i = 0; i < screenMapper.getPixelBufferSize(); i++) {
                int j = screenMapper.attrOffsetFromPixelOffset(i);
                pixbuf[i] = proc.process(pixbuf[i], attrbuf[j], i);
            }
    }

    public void forEachAttr(AttrDataProcessor proc) {
        for (int i = 0; i < screenMapper.getAttrBufferSize(); i++)
                attrbuf[i] = proc.process(attrbuf[i], i);
    }

    void store(OutputStream stream) throws IOException {
        stream.write(pixbuf,0, pixbuf.length);
        stream.write(attrbuf, 0, attrbuf.length);
        //store(stream, 0, 0, SIZE_X, SIZE_Y);
    }

    void load(InputStream stream) throws IOException {
        stream.read(pixbuf,0, pixbuf.length);
        stream.read(attrbuf, 0, attrbuf.length);
    }

/*    void store(OutputStream stream, int x, int y, int width, int height) throws IOException {
        for (int i = x; i < Integer.min(x + width, SIZE_X); i++)
            stream.write(pixbuf[i], y, height);
        for (int i = x / ATTR_FACTOR_X; i < Integer.min((x + width) / ATTR_FACTOR_X, ATTR_SIZE_X); i++)
            stream.write(attrbuf[i], y / ATTR_FACTOR_Y, height / ATTR_FACTOR_Y);
    }

    public void load(InputStream stream) throws IOException {
        load(stream, 256, 192);
    }

    public void load(InputStream stream, int width, int height) throws IOException {
        int ox = (SIZE_X - width) / (2 * ATTR_FACTOR_X) * ATTR_FACTOR_X;
        int oy = (SIZE_Y - height) / (2 * ATTR_FACTOR_Y) * ATTR_FACTOR_Y;
        load(stream, ox, oy, width, height);
    }

    void load(InputStream stream, int ox, int oy, int width, int height) throws IOException {
        byte[] b = new byte[height];
        for (int i = ox; i < width + ox; i++) {
            int v = 0;
            while (v < height)
                v += stream.read(b, v, height - v) ;
            if (i >= 0 && i < SIZE_X) {
                for (int j = oy; j < height + oy; j++) {
                    if (j >= 0 && j < SIZE_Y) pixbuf[i][j] = b[j - oy];
                }
            }
        }

        byte[] a = new byte[height / ATTR_FACTOR_Y];
        for (int i = ox / ATTR_FACTOR_X; i < (ox + width) / ATTR_FACTOR_X; i++) {
            final int h = height / ATTR_FACTOR_Y;
            int v = 0;
            while (v < h)
                v += stream.read(a, v, h - v);
            if (i >= 0 && i < ATTR_SIZE_X) {
                for (int j = oy / ATTR_FACTOR_Y; j < (height + oy) / ATTR_FACTOR_Y; j++) {
                    if (j >= 0 && j < ATTR_SIZE_Y) attrbuf[i][j] = a[j - oy / ATTR_FACTOR_Y];
                }
            }
        }
    }
*/
    void importImage(DataInputStream is) throws IOException {
        Dimension ATTR_SIZE = screenMapper.getAttrSizes();
        int w = is.readInt();
        int h = is.readInt();
        int x = (ATTR_SIZE.width - w) / 2;
        int y = (ATTR_SIZE.height - h) / 2;
        this.loadByTiles(is, x, y, w, h);
    }



    void loadByTiles(InputStream stream, int sx, int sy, int width, int height) throws IOException {
/*        for (int x = sx; x < sx + width; x++)
            for (int y = sy; y < sy + height; y++) {

                for (int yy = 0; yy < ATTR_FACTOR_Y; yy++)
                    for (int xx = 0; xx < ATTR_FACTOR_X; xx++) {
                        final int xi = x * ATTR_FACTOR_X + xx;
                        final int yi = y * ATTR_FACTOR_Y + yy;
                        byte b = (byte) stream.read();
                        if (0 <= xi && xi < SIZE_X && 0 <= yi && yi < SIZE_Y)
                            pixbuf[(xi)][(yi)] = b;
                    }
                byte b = (byte) stream.read();
                if (0 <= x && x < ATTR_SIZE_X && 0 <= y && y < ATTR_SIZE_Y)
                    attrbuf[x][y] = b;
            }
*/    }

}
