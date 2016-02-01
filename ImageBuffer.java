package Painter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ainozemtsev on 23.11.15.
 */
public class ImageBuffer {
    public final int SIZE_X;
    public final int SIZE_Y;

    public final int ATTR_SIZE_X;
    public final int ATTR_SIZE_Y;

    private byte pixbuf[][];
    private byte attrbuf[][];

    public ImageBuffer() {
        this(320,240);
    }

    public ImageBuffer(int sizeX, int sizeY) {
        this.SIZE_X = sizeX;
        this.SIZE_Y = sizeY;
        this.ATTR_SIZE_X = SIZE_X / 8;
        this.ATTR_SIZE_Y = SIZE_Y / 8;
        this.pixbuf = new byte[SIZE_X][SIZE_Y];
        this.attrbuf = new byte[ATTR_SIZE_X][ATTR_SIZE_Y];
    }

    public byte getPixel(int x, int y) {
        return (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y) ? pixbuf[x][y] : -1;
    }

    public byte getAttr(int x, int y) {
        return attrbuf[x / 8][y / 8];
    }

    void putPixel(int x, int y, byte pixel, byte attr) {
        this.pixbuf[x][y] = pixel;
        this.attrbuf[x / 8][y / 8] = attr;
    }

    void store(OutputStream stream) throws IOException {
        store(stream, 0, 0, SIZE_X, SIZE_Y);
    }

    void store(OutputStream stream, int x, int y, int width, int height) throws IOException {
        for (int i = x; i < Integer.min(x + width, SIZE_X); i++)
            stream.write(pixbuf[i], y, height);
        for (int i = x / 8; i < Integer.min((x + width) / 8, ATTR_SIZE_X); i++)
            stream.write(attrbuf[i], y / 8, height / 8);
    }

    void load(InputStream stream) throws IOException {
        load(stream, 256, 192);
    }

    void load(InputStream stream, int width, int height) throws IOException {
        int ox = (SIZE_X - width) / 16 * 8;
        int oy = (SIZE_Y - height) / 16 * 8;
        load(stream, ox, oy, width, height);
    }

    void load(InputStream stream, int ox, int oy, int width, int height) throws IOException {
        byte[] b = new byte[height];
        for (int i = ox; i < width + ox; i++) {
            for (int v = 0; v < height; v += stream.read(b, v, height - v)) ;
            if (i >= 0 && i < SIZE_X) {
                for (int j = oy; j < height + oy; j++) {
                    if (j >= 0 && j < SIZE_Y) pixbuf[i][j] = b[j - oy];
                }
            }
        }

        byte[] a = new byte[height / 8];
        for (int i = ox / 8; i < (ox + width) / 8; i++) {
            final int h = height / 8;
            for (int v = 0; v < h; v += stream.read(a, v, h - v)) ;
            if (i >= 0 && i < ATTR_SIZE_X) {
                for (int j = oy / 8; j < (height + oy) / 8; j++) {
                    if (j >= 0 && j < ATTR_SIZE_Y) attrbuf[i][j] = a[j - oy / 8];
                }
            }
        }
    }

    void loadByTiles(InputStream stream, int sx, int sy, int width, int height) throws IOException {
        for (int x = sx; x < sx + width; x++)
            for (int y = sy; y < sy + height; y++) {

                for (int yy = 0; yy < 8; yy++)
                    for (int xx = 0; xx < 8; xx++) {
                        final int xi = x * 8 + xx;
                        final int yi = y * 8 + yy;
                        if (xi < SIZE_X && yi < SIZE_Y)
                            pixbuf[(xi)][(yi)] = (byte) stream.read();
                    }
                if (x < ATTR_SIZE_X && y < ATTR_SIZE_Y)
                    attrbuf[x][y] = (byte) stream.read();
            }
    }

}
