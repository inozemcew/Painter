package Painter.Screen;

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
    public final int SIZE_X, SIZE_Y;
    //private final Dimension pixelFactor;
    private final int ATTR_SIZE_X, ATTR_SIZE_Y;
    private final int ATTR_FACTOR_X, ATTR_FACTOR_Y;
    private byte pixbuf[][], attrbuf[][];

    public ImageBuffer(int sizeX, int sizeY, Dimension pixelFactor, Dimension attrFactor) {
        //this.pixelFactor = new Dimension(pixelFactor);
        this.SIZE_X = sizeX / pixelFactor.width;
        this.SIZE_Y = sizeY / pixelFactor.height;
        this.ATTR_FACTOR_X = attrFactor.width / pixelFactor.width;
        this.ATTR_FACTOR_Y = attrFactor.height / pixelFactor.height;
        this.ATTR_SIZE_X = SIZE_X / ATTR_FACTOR_X;
        this.ATTR_SIZE_Y = SIZE_Y / ATTR_FACTOR_Y;
        this.pixbuf = createPixbuf();
        this.attrbuf = new byte[ATTR_SIZE_X][ATTR_SIZE_Y];
    }

    protected byte[][] createPixbuf() {
        return new byte[SIZE_X][SIZE_Y];
    }

    public byte getPixel(int x, int y) {
        return (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y) ? pixbuf[x][y] : -1;
    }

    public byte getAttr(int x, int y) {
        return attrbuf[x / ATTR_FACTOR_X][y / ATTR_FACTOR_Y];
    }

    public void putPixel(int x, int y, byte pixel) {
        this.pixbuf[x][y] = pixel;
    }

    public void putPixel(int x, int y, byte pixel, byte attr) {
        putPixel(x, y, pixel);
        this.attrbuf[x / ATTR_FACTOR_X][y / ATTR_FACTOR_Y] = attr;
    }

    void shift (int dx, int dy) {
        byte newPixBuf[][] = createPixbuf();
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < SIZE_X && ny >= 0 && ny <SIZE_Y) {
                    newPixBuf[nx][ny] = pixbuf[x][y];
                }
            }
        }
        pixbuf = newPixBuf;
    }

    @FunctionalInterface
    public interface PixelProcessor {
        byte process(int x, int y, byte b, byte a);
    }

    @FunctionalInterface
    public interface AttrProcessor {
        byte process(int x, int y, byte b);
    }

    public void forEachPixel(PixelProcessor proc) {
        for (int x = 0; x < SIZE_X; x++) {
            int xx = x / ATTR_FACTOR_X;
            for (int y = 0; y < SIZE_Y; y++){
                int yy = y / ATTR_FACTOR_Y;
                pixbuf[x][y] = proc.process(x, y, pixbuf[x][y], attrbuf[xx][yy]);
            }
        }
    }

    public void forEachAttr(AttrProcessor proc) {
        for (int x = 0; x < ATTR_SIZE_X; x++)
            for (int y = 0; y < ATTR_SIZE_Y; y++)
                attrbuf[x][y] = proc.process(x, y, attrbuf[x][y]);
    }

    void store(OutputStream stream) throws IOException {
        store(stream, 0, 0, SIZE_X, SIZE_Y);
    }

    void store(OutputStream stream, int x, int y, int width, int height) throws IOException {
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

    void importImage(DataInputStream is) throws IOException {
        int w = is.readInt() / this.ATTR_FACTOR_X;
        int h = is.readInt() / this.ATTR_FACTOR_Y;
        int x = (this.ATTR_SIZE_X - w) / 2;
        int y = (this.ATTR_SIZE_Y - h) / 2;
        this.loadByTiles(is, x, y, w, h);
    }



    void loadByTiles(InputStream stream, int sx, int sy, int width, int height) throws IOException {
        for (int x = sx; x < sx + width; x++)
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
    }

}
