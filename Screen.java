package Painter;

import Painter.Palette.Palette;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Created by aleksey on 22.01.16.
 * Screen as virtual device for painting on
 * Unites screen buffer and palette
 */
public abstract class Screen implements ImageSupplier {
    protected ImageBuffer image;
    protected Palette palette = new Palette();
    protected Enum mode = null;
    private Collection<ImageChangeListener> listeners = new ArrayList<>();
    protected final UndoRedo undo = new UndoRedo();
    protected int enhancedInk = -1, enhancedPaper = -1;
    protected int GRID_FACTOR_X = 8, GRID_FACTOR_Y = 8;

    private int locked = 0;

    public Screen() {
        image = createImageBuffer();
        palette = createPalette();
    }

    protected ImageBuffer createImageBuffer() {
        return new ImageBuffer();
    }

    protected ImageBuffer createImageBuffer(int x, int y) {
        return new ImageBuffer(x,y);
    }

    protected Palette createPalette() {
        return new Palette();
    }

    @Override
    public void addChangeListener(ImageChangeListener listener) {
        this.listeners.add(listener);
        palette.addChangeListener(listener);
    }

    @Override
    abstract public Color getPixelColor(int x, int y) ;

    @Override
    abstract public ImageSupplier.Status getStatus(int x, int y);

    abstract public Pixel getPixelDescriptor(int x, int y);

    public void setEnhanced(int ink, int paper) {
        this.enhancedInk = ink;
        this.enhancedPaper = paper;
        fireImageChanged();
    }

    public Enum getMode() {
        return mode;
    }

    public void setMode(Enum mode) {
        this.mode = mode;
        fireImageChanged();
    }

    @Override // in ImageSupplier
    public int getImageHeight() {
        return image.SIZE_Y;
    }

    @Override // in ImageSupplier
    public int getImageWidth() {
        return image.SIZE_X;
    }

    public Palette getPalette() {
        return palette;
    }

    void newImageBuffer(int sizeX, int sizeY) {
        this.image = createImageBuffer(sizeX, sizeY);
        fireImageChanged();
    }

    private boolean isInImage(Point p) {
        return isInImage(p.x, p.y);
    }

    boolean isInImage(int x, int y) {
        return x >= 0 && x < getImageWidth() && y >= 0 && y < getImageHeight();
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
                putPixel(e.x, e.y, e.pixel, e.attr);
            }
        }
    }

    public void redoDraw() {
        Vector<UndoElement> elements = undo.redo();
        if (null != elements) {
            for (UndoElement e : elements) {
                putPixel(e.x, e.y, e.newPixel, e.newAttr);
            }
        }
    }

    abstract protected  byte attrFromDesc(Pixel pixel, byte oldAttr);
    abstract protected  byte pixelFromDesc(Pixel pixel, byte oldPixel, int x, int y);

    protected byte getPixel(int x, int y) {
        return image.getPixel(x,y);
    }

    protected byte getAttr(int x, int y) {
        return image.getAttr(x, y);
    }

    protected void putPixel(int x, int y, byte pixel, byte attr) {
        image.putPixel(x, y, pixel, attr);
    }

    void setPixel(int x, int y, Pixel pixel) {
        if (isInImage(x, y)) {
            byte a =  getAttr(x, y);
            if (pixel.index >= 0)
                a = attrFromDesc(pixel, a);
            byte b = pixelFromDesc(pixel, getPixel(x, y), x, y);
            undo.add(x, y, getPixel(x, y), getAttr(x, y), b, a);
            putPixel(x, y, b, a);
            fireImageChanged(x / GRID_FACTOR_X * GRID_FACTOR_X, y / GRID_FACTOR_Y * GRID_FACTOR_Y, GRID_FACTOR_X, GRID_FACTOR_Y);
        }
    }

    void drawLine(int ox, int oy, int x, int y, Pixel pixel) {
        if (isInImage(x, y) && isInImage(ox, oy)) {
            lock();
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
                setPixel((int) xi, (int) yi, pixel);
            }

            int sx, sy, w, h;
            if (ox < x) {
                sx = ox / GRID_FACTOR_X * GRID_FACTOR_X;
                w = x / GRID_FACTOR_X * GRID_FACTOR_X + GRID_FACTOR_X - sx;
            } else {
                sx = x / GRID_FACTOR_X * GRID_FACTOR_X;
                w = ox / GRID_FACTOR_X * GRID_FACTOR_X + GRID_FACTOR_X - sx;
            }
            if (oy < y) {
                sy = oy / GRID_FACTOR_Y * GRID_FACTOR_Y;
                h = y / GRID_FACTOR_Y * GRID_FACTOR_Y + GRID_FACTOR_Y - sy;
            } else {
                sy = y / GRID_FACTOR_Y * GRID_FACTOR_Y;
                h = oy / GRID_FACTOR_Y * GRID_FACTOR_Y + GRID_FACTOR_Y - sy;
            }
            unlock();
            fireImageChanged(sx, sy, w, h);
        }
    }

    protected int getPointSize() {
        return 1;
    }

    void fill(int x, int y, Pixel pixel) {
        if (isInImage(x, y)) {
            lock();
            beginDraw();
            Stack<Point> stack = new Stack<>();
            Pixel[][] pixels = new Pixel[getImageWidth()][getImageHeight()];
            final int ss = 1; getPointSize();
            stack.push(new Point(x, y));
            Pixel pix = getPixelDescriptor(x, y);
            while (!stack.empty()) {
                Point p = stack.pop();
                Pixel pix2 = getPixelDescriptor(p.x, p.y);
                if (pix2.hasSameColor(pixel,palette) || !pix2.hasSameColor(pix,palette) || pixels[p.x][p.y]!=null)
                    continue;
                pixels[p.x][p.y] = pixel;
                //setPixel(p.x, p.y, pixel);
                if (p.x > 0) stack.push(new Point(p.x - ss, p.y));
                if (p.y > 0) stack.push(new Point(p.x, p.y - ss));
                if (p.x < getImageWidth() - 1) stack.push(new Point(p.x + ss, p.y));
                if (p.y < getImageHeight() - 1) stack.push(new Point(p.x, p.y + ss));
            }
            for (int xx = 0; xx < getImageWidth(); xx++)
                for (int yy = 0; yy < getImageHeight(); yy++) {
                    if (pixels[xx][yy] != null)
                        setPixel(xx,yy,pixels[xx][yy]);
                }

            endDraw();
            unlock();
            fireImageChanged();
        }
    }

    void copyCell(int fx, int fy, int tx, int ty) {
        if (isInImage(fx*8, fy*8) && isInImage(tx*8,ty*8)) {
            lock();
            beginDraw();
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    Pixel p = getPixelDescriptor(fx*8+x, fy*8+y);
                    setPixel(tx*8+x, ty*8+y, p);
                }
            }
            endDraw();
            unlock();
            fireImageChanged(tx*8,ty*8,8,8);
        }
    }

    protected void rearrangeColorTable(Palette.Table table, int[] order) {

    }

    boolean isLocked() {
        return locked !=0;
    }

    void lock() {
        locked++;
    }

    void unlock() {
        if (isLocked()) locked--;
    }

    protected void fireImageChanged() {
        if (isLocked()) return;
        listeners.forEach(ImageChangeListener::imageChanged);
    }

    private void fireImageChanged(int x, int y, int w, int h) {
        if (isLocked()) return;
        listeners.forEach(l -> l.imageChanged(x, y, w, h));
    }

    public void swapColors(Palette.Table table, int from, int to) {
        final int length = palette.getPalette(table).length;
        int[] order = new int[length];
        int j = 0;
        for (int i = 0; i < length; i++) {
            if (i == from) order[i] = to; else {
                if (j == to) j++;
                order[i] = j;
                j++;
            }
        }
        rearrangeColorTable(table, order);
    }

    enum Shift {
        Left(-1,0), Right(1,0), Up(0,-1), Down(0,1);
        private int dx, dy;
        Shift(int x, int y) {
            this.dx = x;
            this.dy = y;
        };
    }

    void shift(Shift shift) {
        image.shift(shift.dx, shift.dy);
        fireImageChanged();
    }

    public  Map<String, BiConsumer<Integer,Integer>> getSpecialMethods() {
        return new HashMap<>();
    }

    abstract public Map<String,Dimension> getResolutions();

    abstract protected FileNameExtensionFilter getFileNameExtensionFilter();

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
            image.load(stream);
            ink = (int[]) stream.readObject();
            paper = (int[]) stream.readObject();

        } else {
            //stream.reset();
            ink = (int[]) stream.readObject();
            paper = (int[]) stream.readObject();
            int x = stream.readInt();
            int y = stream.readInt();
            image.load(stream,x,y);
        }
        getPalette().setPalette(ink, paper);
        stream.close();
    }

    public abstract void importSCR(InputStream stream) throws IOException;

    public void importImage(DataInputStream is) throws IOException {
        palette.loadPalette(is);
        int w = is.readInt() / image.ATTR_FACTOR_X;
        int h = is.readInt() / image.ATTR_FACTOR_Y;
        int x = (image.ATTR_SIZE_X - w) / 2;
        int y = (image.ATTR_SIZE_Y - h) / 2;
        image.loadByTiles(is, x, y, w, h);
        is.close();
    }

    public static class Pixel {
        public final Palette.Table table;
        public final int index;
        public final int shift;

        public Pixel(Palette.Table table, int index, int shift) {
            this.index = index;
            this.shift = shift;
            this.table = table;
        }

        public Pixel clone() {
            return new Pixel(this.table, this.index,this.shift);
        }

        public boolean equals(Pixel other) {
            return (this.table == other.table) && (this.index == other.index) && (this.shift == other.shift);
        }

        public boolean hasSameColor(Pixel other, Palette palette) {
            return palette.getRGBColor(table,index,shift).equals(palette.getRGBColor(other.table,other.index,other.shift));
        }

    }
}

