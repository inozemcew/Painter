package Painter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ainozemtsev on 23.11.15.
 * Image buffer for 2 bitplanes
 */
public class ImageBuffer {

    protected final int CELL_SIZE_X = 8;
    protected final int CELL_SIZE_Y = 8;

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
        this.ATTR_SIZE_X = SIZE_X / CELL_SIZE_X;
        this.ATTR_SIZE_Y = SIZE_Y / CELL_SIZE_Y;
        this.pixbuf = new byte[SIZE_X][SIZE_Y];
        this.attrbuf = new byte[ATTR_SIZE_X][ATTR_SIZE_Y];
    }

    public byte getPixel(int x, int y) {
        return (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y) ? pixbuf[x][y] : -1;
    }

    public byte getAttr(int x, int y) {
        return attrbuf[x / CELL_SIZE_X][y / CELL_SIZE_Y];
    }

    void putPixel(int x, int y, byte pixel, byte attr) {
        this.pixbuf[x][y] = pixel;
        this.attrbuf[x / CELL_SIZE_X][y / CELL_SIZE_Y] = attr;
    }

    void shift (int dx, int dy) {
        byte newPixBuf[][] = new byte[SIZE_X][SIZE_Y];
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
    interface PixelProcessor {
        byte process(int x, int y, byte b, byte a);
    }

    @FunctionalInterface
    interface AttrProcessor {
        byte process(int x, int y, byte b);
    }

    void forEachPixel(PixelProcessor proc) {
        for (int x = 0; x < SIZE_X; x++) {
            int xx = x / CELL_SIZE_X;
            for (int y = 0; y < SIZE_Y; y++){
                int yy = y / CELL_SIZE_Y;
                pixbuf[x][y] = proc.process(x, y, pixbuf[x][y], attrbuf[xx][yy]);
            }
        }
    }

    void forEachAttr(AttrProcessor proc) {
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
        for (int i = x / CELL_SIZE_X; i < Integer.min((x + width) / CELL_SIZE_X, ATTR_SIZE_X); i++)
            stream.write(attrbuf[i], y / CELL_SIZE_Y, height / CELL_SIZE_Y);
    }

    void load(InputStream stream) throws IOException {
        load(stream, 256, 192);
    }

    void load(InputStream stream, int width, int height) throws IOException {
        int ox = (SIZE_X - width) / (2 * CELL_SIZE_X) * CELL_SIZE_X;
        int oy = (SIZE_Y - height) / (2 * CELL_SIZE_Y) * CELL_SIZE_Y;
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

        final int h = height / CELL_SIZE_Y;
        byte[] a = new byte[h];
        for (int i = ox / CELL_SIZE_X; i < (ox + width) / CELL_SIZE_X; i++) {
            int v = 0;
            while (v < h)
                v += stream.read(a, v, h - v);
            if (i >= 0 && i < ATTR_SIZE_X) {
                for (int j = oy / CELL_SIZE_Y; j < (height + oy) / CELL_SIZE_Y; j++) {
                    if (j >= 0 && j < ATTR_SIZE_Y) attrbuf[i][j] = a[j - oy / CELL_SIZE_Y];
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
