package Painter.Screen;

import Painter.Palette.Palette;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by aleksey on 22.01.16.
 * Screen as virtual device for painting on
 * Unites screen buffer and palette
 */
public abstract class Screen implements ImageSupplier {
    protected ImageBuffer image;
    protected Palette palette;
    protected Enum mode = null;
    private Collection<ImageChangeListener> listeners = new ArrayList<>();
    protected final UndoRedo undo = new UndoRedo();
    protected int[] enhancedColors;
    protected Dimension GRID_FACTOR = new Dimension();
    protected Dimension pixelFactor = new Dimension();
    protected Dimension attrFactor = new Dimension();

    private int locked = 0;

    public Screen() {
        this(320,240);
    }

    public Screen(int w, int h) {
        setFactors();
        image = createImageBuffer(w,h);
        palette = createPalette();
        enhancedColors = new int[palette.getTablesCount()];
        resetEnhanced();
    }

    protected ImageBuffer createImageBuffer(int w, int h) {
        return new ImageBuffer(w, h, pixelFactor, attrFactor);
    }

    abstract protected Palette createPalette();

    abstract protected void setFactors();

    public Dimension getGridFactor() {
        return GRID_FACTOR;
    }

    @Override
    public void addChangeListener(ImageChangeListener listener) {
        this.listeners.add(listener);
        palette.addChangeListener(listener);
    }

    @Override // in ImageSupplier
    public int getImageHeight() {
        return image.SIZE_Y * pixelFactor.height;
    }

    @Override // in ImageSupplier
    public int getImageWidth() {
        return image.SIZE_X * pixelFactor.width;
    }

    @Override
    abstract public Color getPixelColor(int x, int y) ;

    @Override
    abstract public ImageSupplier.Status getStatus(int x, int y);

    abstract public Pixel getPixelDescriptor(int x, int y);

    abstract public Enum mapColorTable(int table);

    public void setEnhanced(int[] colors) {
        this.enhancedColors = colors;
        fireImageChanged();
    }

    public void resetEnhanced() {
        int[] e = new int[enhancedColors.length];
        Arrays.fill(e,-1);
        setEnhanced(e);
    }

    public Enum getMode() {
        return mode;
    }

    public void setMode(Enum mode) {
        this.mode = mode;
        fireImageChanged();
    }

    public Palette getPalette() {
        return palette;
    }

    public void newImageBuffer(int sizeX, int sizeY) {
        this.image = createImageBuffer(sizeX, sizeY);
        fireImageChanged();
    }

    private boolean isInImage(Point p) {
        return isInImage(p.x, p.y);
    }

    public boolean isInImage(int x, int y) {
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
                putPixelData(e.x, e.y, e.pixel, e.attr);
            }
        }
    }

    public void redoDraw() {
        Vector<UndoElement> elements = undo.redo();
        if (null != elements) {
            for (UndoElement e : elements) {
                putPixelData(e.x, e.y, e.newPixel, e.newAttr);
            }
        }
    }

    abstract protected  byte attrFromDesc(Pixel pixel, byte oldAttr);
    abstract protected  byte pixelFromDesc(Pixel pixel, byte oldPixel, int x, int y);

    protected byte getPixelData(int x, int y) {
        final int xx = x / pixelFactor.width;
        final int yy = y / pixelFactor.height;
        return image.getPixel(xx,yy);
    }

    protected byte getAttr(int x, int y) {
        final int xx = x / pixelFactor.width;
        final int yy = y / pixelFactor.height;
        return image.getAttr(xx, yy);
    }

    protected void putPixelData(int x, int y, byte pixel, byte attr) {
        final int xx = x / pixelFactor.width;
        final int yy = y / pixelFactor.height;
        image.putPixel(xx, yy, pixel, attr);
    }

    protected void putPixelData(int x, int y, byte pixel) {
        final int xx = x / pixelFactor.width;
        final int yy = y / pixelFactor.height;
        image.putPixel(xx, yy, pixel);
    }


    public void setPixel(int x, int y, Pixel pixel) {
        if (isInImage(x, y)) {
            byte a =  getAttr(x, y);
            if (pixel.index >= 0)
                a = attrFromDesc(pixel, a);
            byte b = pixelFromDesc(pixel, getPixelData(x, y), x, y);
            undo.add(x, y, getPixelData(x, y), getAttr(x, y), b, a);
            putPixelData(x, y, b, a);
            fireImageChanged(alignX(x), alignY(y), GRID_FACTOR.width, GRID_FACTOR.height);
        }
    }

    public void drawLine(int ox, int oy, int x, int y, Pixel pixel) {
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
                sx = alignX(ox);
                w = alignX(x) + GRID_FACTOR.width - sx;
            } else {
                sx = alignX(x);
                w = alignX(ox) + GRID_FACTOR.width - sx;
            }
            if (oy < y) {
                sy = alignY(oy);
                h = alignY(y) + GRID_FACTOR.height - sy;
            } else {
                sy = alignY(y);
                h = alignY(oy) + GRID_FACTOR.height - sy;
            }
            unlock();
            fireImageChanged(sx, sy, w, h);
        }
    }

    protected int getPointSize() {
        return 1;
    }

    public void fill(int x, int y, Pixel pixel) {
        if (isInImage(x, y)) {
            lock();
            beginDraw();
            Stack<Point> stack = new Stack<>();
            Pixel[][] pixels = new Pixel[getImageWidth()][getImageHeight()];
            final int ss = 1;
            stack.push(new Point(x, y));
            Pixel pix = getPixelDescriptor(x, y);
            while (!stack.empty()) {
                Point p = stack.pop();
                Pixel pix2 = getPixelDescriptor(p.x, p.y);
                if (!pix2.hasSameColor(pix,palette) || pixels[p.x][p.y]!=null)
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

    public void copyCell(int fx, int fy, int tx, int ty) {
        copyCell(this,fx,fy,tx,ty);
    }


    public void copyCell(Screen source, int fx, int fy, int tx, int ty) {
        int fxx = alignX(fx);
        int fyy = alignY(fy);
        int txx = alignX(tx);
        int tyy = alignY(ty);

        if (source.isInImage(fxx, fyy) && isInImage(txx, tyy)) {
            lock();
            beginDraw();
            byte a = source.getAttr(fxx, fyy);
            for (int x = 0; x < GRID_FACTOR.width; x++) {
                for (int y = 0; y < GRID_FACTOR.height; y++) {
                    byte b = source.getPixelData(fxx + x, fyy + y);
                    undo.add(txx + x, tyy + y,getPixelData(txx + x, tyy + y),getAttr(txx + x, tyy + y),b,a);
                    putPixelData(txx + x, tyy + y, b, a);
                }
            }
            endDraw();
            unlock();
            fireImageChanged(txx, tyy, GRID_FACTOR.width, GRID_FACTOR.height);
        }
    }

    public int alignX(int x) {
        return x / GRID_FACTOR.width * GRID_FACTOR.width;
    }

    public int alignY(int y) {
        return y / GRID_FACTOR.height * GRID_FACTOR.height;
    }

    protected void rearrangeColorTable(int table, int[] order) {

    }

    private boolean isLocked() {
        return locked !=0;
    }

    private void lock() {
        locked++;
    }

    private void unlock() {
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

    public void swapColors(int table, int from, int to) {
        final int length = palette.getColorsCount(table);
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

    public enum Shift {
        Left(-1,0), Right(1,0), Up(0,-1), Down(0,1);
        private int dx, dy;
        Shift(int x, int y) {
            this.dx = x;
            this.dy = y;
        }
    }

    public void shift(Shift shift) {
        image.shift(shift.dx, shift.dy);
        fireImageChanged();
    }

    public  Map<String, Consumer<Integer[]>> getSpecialMethods() {
        return new HashMap<>();
    }

    abstract public Map<String,Dimension> getResolutions();

    abstract public FileNameExtensionFilter getFileNameExtensionFilter();

    public void save(ObjectOutputStream stream) throws IOException {
        palette.savePalette(stream);
        stream.writeInt(image.SIZE_X);
        stream.writeInt(image.SIZE_Y);
        image.store(stream);
    }

    public void load(ObjectInputStream stream, boolean old) throws IOException, ClassNotFoundException {
        int[] ink;
        int[] paper;
        if (old) {
            image.load(stream);
            getPalette().loadPalette(stream);

        } else {
            getPalette().loadPalette(stream);
            int x = stream.readInt();
            int y = stream.readInt();
            image.load(stream,x,y);
        }
        stream.close();
    }

    public abstract void importSCR(InputStream stream) throws IOException;

    public void importImage(DataInputStream is) throws IOException {
        palette.loadPalette(is);
        image.importImage(is);
        is.close();
    }

    public static class Pixel {
        public final Enum table;
        public final int index;
        public final int shift;

        public Pixel(Enum table, int index, int shift) {
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

