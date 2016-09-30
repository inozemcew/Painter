package Painter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ainozemtsev on 07.07.16.
 */
public class SpectrumScreen {
    public class Pixel {
        final public boolean value;
        final public int ink;
        final public int bright;
        final public int paper;

        public Pixel(int ink, int paper, int bright, boolean value) {
            this.ink = ink;
            this.paper = paper;
            this.bright = bright;
            this.value = value;
        }
    }

    private byte[][] pixels = new byte[192][32];
    private byte[][] attrs = new byte[24][32];
    private int offsetX = 0, offsetY = 0;

    public SpectrumScreen(InputStream stream) throws IOException {
        for (int z = 0; z < 3; z++) {
            for (int y = 0; y < 8; y++) {
                for (int l = 0; l < 8; l++) {
                    stream.read(pixels[z * 64 + l * 8 + y]);
                }
            }
        }
        for (int y = 0; y < 24; y++) {
            stream.read(attrs[y]);
        }
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    protected int mapX(int x) {
        return x + offsetX;
    }

    protected int mapY(int y) {
        return y + offsetY;
    }

    public Pixel getPixel(int x, int y) {
        int mx = mapX(x), my = mapY(y);
        if (mx < 0 || mx >= 256 || my < 0 || my >= 192)
            return null;
        int xx = mx / 8, yy = my / 8;
        byte b = pixels[my][xx];
        int p = (b >> (7 - mx + xx * 8)) & 1;
        byte a = attrs[yy][xx];
        int ink = a & 7;
        int paper = (a & 0x38) >> 3;
        int bright = (a & 0x40) >> 6;
        return new Pixel(ink, paper, bright, p != 0);
    }

    public int getChunk(int x, int y) {
        byte[] a1 = pixels[y + offsetY];
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (a1[x + i + offsetX] & 255);
        }
        return result;
    }
}
