package Painter;

import Painter.Convert.ConvertDialog;
import Painter.Convert.ImageConverter;
import Painter.Palette.Palette;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * Created by aleksey on 22.01.16.
 */
public class Screen implements ImageSupplier {
    private ImageBuffer image = new ImageBuffer();
    private Palette palette = new Palette();
    private Collection<ImageChangeListener> listeners = new ArrayList<>();
    private final UndoRedo undo = new UndoRedo();

    @Override
    public void addChangeListener(ImageChangeListener listener) {
        this.listeners.add(listener);
        palette.addChangeListener(listener);
    }

    @Override
    public Color getPixelColor(int x, int y) {
        int attr = image.getAttr(x, y);
        int pix = image.getPixel(x, y);
        if (pix < 2)
            return palette.getPaperColor((attr >> 3) & 7, pix & 1);
        else
            return palette.getInkColor(attr & 7, pix & 1);
    }

    @Override
    public int getImageHeight() {
        return image.SIZE_Y;
    }

    @Override
    public int getImageWidth() {
        return image.SIZE_X;
    }

    Palette getPalette() {
        return palette;
    }

    void newImageBuffer(int sizeX, int sizeY) {
        this.image = new ImageBuffer(sizeX,sizeY);
        listeners.forEach(ImageChangeListener::imageChanged);
    }

    ImageBuffer getImage() {
        return image;
    }

    String getPixelDescription(int x, int y) {
        int v = image.getPixel(x, y);
        byte attr = image.getAttr(x, y);
        return ((v < 2) ? "Paper" : "Ink") + String.valueOf(v & 1)
                + "=" + String.valueOf(((v < 2) ? attr >> 3 : attr ) & 7);
    }

    private boolean isInImage(Point p) {
        return isInImage(p.x, p.y);
    }

    boolean isInImage(int x, int y) {
        return x >= 0 && x < image.SIZE_X && y >= 0 && y < image.SIZE_Y;
    }

    public void beginDraw() {
        undo.start();
    }

    public void endDraw() {
        undo.commit();
    }

    public void undoDraw() {
        Vector<UndoElement> elements = undo.undo();
        if (null != elements) {
            ListIterator<UndoElement> i = elements.listIterator(elements.size());
            while (i.hasPrevious()) {
                UndoElement e = i.previous();
                image.putPixel(e.x, e.y, e.pixel, e.attr);
            }
        }
    }

    public void redoDraw() {
        Vector<UndoElement> elements = undo.redo();
        if (null != elements) {
            for (UndoElement e : elements) {
                image.putPixel(e.x, e.y, e.newPixel, e.newAttr);
            }
        }
    }


    void setPixel(int x, int y, Palette.Table table, int index, byte shift) {
        if (isInImage(x, y)) {
            //image.setPixel(x, y, table, index, shift);
            byte a = image.getAttr(x, y);
            byte s;
            if (table == Palette.Table.INK) {
                if (index >= 0) a = (byte) ((a & 0x38) | index);
                s = 2;
            } else {
                if (index >= 0) a = (byte) ((a & 7) | (index << 3));
                s = 0;
            }
            s = (byte) (shift | s);

            undo.add(x, y, image.getPixel(x, y), image.getAttr(x, y), s, a);
            image.putPixel(x, y, s, a);
            listeners.forEach(l -> l.imageChanged(x / 8 * 8, y / 8 * 8, 8, 8));
        }
    }

    void drawLine(int ox, int oy, int x, int y, Palette.Table table, int index, byte shift) {
        if (isInImage(x, y) && isInImage(ox, oy)) {
            float dx = x - ox, dy = y - oy;
            if (Math.abs(dx) < Math.abs(dy)) {
                dx = dx / Math.abs(dy);
                dy = Math.signum(dy);
            } else {
                dy = dy / Math.abs(dx);
                dx = Math.signum(dx);
            }
            float xi = ox, yi = oy;
            while (Math.round(xi) != x || Math.round(yi) != y) {
                xi += dx;
                yi += dy;
                setPixel((int) xi, (int) yi, table, index, shift);
            }

            int sx, sy, w, h;
            if (ox < x) {
                sx = ox / 8 * 8;
                w = x / 8 * 8 + 8 - sx;
            } else {
                sx = x / 8 * 8;
                w = ox / 8 * 8 + 8 - sx;
            }
            if (oy < y) {
                sy = oy / 8 * 8;
                h = y / 8 * 8 + 8 - sy;
            } else {
                sy = y / 8 * 8;
                h = oy / 8 * 8 + 8 - sy;
            }
            listeners.forEach(l -> l.imageChanged(sx, sy, w, h));
        }
    }

    void fill(int x, int y, Palette.Table table, int index, byte shift) {
        if (isInImage(x, y)) {
            beginDraw();
            Stack<Point> stack = new Stack<>();
            stack.push(new Point(x, y));
            int pix = image.getPixel(x, y);
            int npix = (table == Palette.Table.INK) ? (2 | shift) : shift;
            while (!stack.empty()) {
                Point p = stack.pop();
                int pixel = image.getPixel(p.x, p.y);
                if (pixel == npix || pixel != pix) continue;
                setPixel(p.x, p.y, table, index, shift);
                if (p.x > 0) stack.push(new Point(p.x - 1, p.y));
                if (p.y > 0) stack.push(new Point(p.x, p.y - 1));
                if (p.x < image.SIZE_X - 1) stack.push(new Point(p.x + 1, p.y));
                if (p.y < image.SIZE_Y - 1) stack.push(new Point(p.x, p.y + 1));
            }
            endDraw();
            listeners.forEach(ImageChangeListener::imageChanged);
        }
    }

    void save(ObjectOutputStream stream) throws IOException {
        stream.writeObject(getPalette().getPalette(Palette.Table.INK));
        stream.writeObject(getPalette().getPalette(Palette.Table.PAPER));
        stream.writeInt(image.SIZE_X);
        stream.writeInt(image.SIZE_Y);
        image.store(stream);
    }
    public void load(ObjectInputStream stream, boolean old) throws IOException, ClassNotFoundException {
        int[] ink;
        int[] paper;
        if (old) {
            getImage().load(stream);
            ink = (int[]) stream.readObject();
            paper = (int[]) stream.readObject();

        } else {
            //stream.reset();
            ink = (int[]) stream.readObject();
            paper = (int[]) stream.readObject();
            int x = stream.readInt();
            int y = stream.readInt();
            getImage().load(stream,x,y);
        }
        getPalette().setPalette(ink, paper);
        stream.close();
    }

    public void importSCR(InputStream stream) throws IOException {
        byte[] pix = new byte[2048 * 3];
        byte[] attr = new byte[768];
        stream.read(pix);
        stream.read(attr);
        ByteArrayOutputStream as = new ByteArrayOutputStream(256 * 192 + 32 * 24);
        boolean bright;
        for (int x = 0; x < 256; x++) {
            byte[] buf = new byte[192];
            for (int y = 0; y < 192; y++) {
                byte a = attr[(y & 0xf8) * 4 + x / 8];
                bright = (a & 0x40) != 0;
                int b = pix[(x >> 3) + 256 * (y & 7) + 4 * (y & 0x38) + 32 * (y & 0xc0)] >> (7 - (x & 7));
                byte p = (byte) ((b & 1) * 2 + (bright ? 0 : 1));
                if ((a & 7) == 0 && p == 3) p = 2;
                if ((a & 0x38) == 0 && p == 1) p = 0;
                buf[y] = p;
            }
            as.write(buf);
        }
        for (int x = 0; x < 32; x++) {
            byte[] buf = new byte[24];
            for (int y = 0; y < 24; y++) {
                byte a = attr[x + 32 * y];
                buf[y] = (byte) (a & 0x3f);
            }
            as.write(buf);
        }
        image.load(new ByteArrayInputStream(as.toByteArray()));

        int[] ink = {0x00, 0x14, 0x18, 0x1e, 0x1c, 0x1a, 0x16, 0x12};
        for (int i = 1; i < 8; i++) ink[i] += (ink[i] + 0x10) << 6;
        palette.setPalette(ink, ink);

    }

    void importPNG(InputStream stream) throws IOException {
        ImageConverter converter = new ImageConverter(ImageIO.read(stream));
        ConvertDialog convertDialog = new ConvertDialog(converter);
        if (!convertDialog.runDialog()) return;
        DataInputStream is = converter.asTileStream();
        palette.loadPalette(is);
        int w = is.readInt() / 8;
        int h = is.readInt() / 8;
        int x = (image.ATTR_SIZE_X - w) / 2;
        int y = (image.ATTR_SIZE_Y - h) / 2;
        image.loadByTiles(is, (x < 0) ? 0 : x, (y < 0) ? 0 : y, w, h);
        is.close();
    }

}
