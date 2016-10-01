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
    protected PixelProcessing pixelProcessor = null;
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
        pixelProcessor = createPixelProcessor();
        enhancedColors = new int[palette.getTablesCount()];
        resetEnhanced();
    }

    protected ImageBuffer createImageBuffer(int w, int h) {
        return new ImageBuffer(w, h, pixelFactor, attrFactor);
    }

    abstract protected Palette createPalette();

    abstract protected void setFactors();

    abstract protected PixelProcessing createPixelProcessor();

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
    public Color getPixelColor(Point pos){
        Pixel pixel = getPixel(pos);
        return pixelProcessor.getPixelColor(pixel, palette);
    }

    public Pixel getPixel(Point pos) {
        byte data = getPixelData(pos);
        byte attr = getAttr(pos);
        return pixelProcessor.unpackPixel(data, attr, pos);
    }

    @Override
    abstract public ImageSupplier.Status getStatus(Point pos);

    //abstract public Pixel getPixelDescriptor(Point pos);

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

    public PixelProcessing getPixelProcessor() {
        return pixelProcessor;
    }

    public void setPixelProcessing(PixelProcessing processor) {
        this.pixelProcessor = processor;
        fireImageChanged();
    }

    public Palette getPalette() {
        return palette;
    }

    public void newImageBuffer(int sizeX, int sizeY) {
        this.image = createImageBuffer(sizeX, sizeY);
        fireImageChanged();
    }

    public boolean isInImage(Point p) {
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
                putPixelData(e.pos, e.pixel, e.attr);
            }
        }
    }

    public void redoDraw() {
        Vector<UndoElement> elements = undo.redo();
        if (null != elements) {
            for (UndoElement e : elements) {
                putPixelData(e.pos, e.newPixel, e.newAttr);
            }
        }
    }

    //abstract protected  byte attrFromDesc(Pixel pixel, byte oldAttr);
    //abstract protected  byte pixelFromDesc(Pixel pixel, byte oldPixel, int x, int y);

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


    public void setPixel(Pixel pixel, Point pos) {
        if (isInImage(pos)) {
            byte a =  getAttr(pos);
            if (pixel.index >= 0)
                a = pixelProcessor.packAttr(pixel, a,pos);
            byte oldPixelData = getPixelData(pos);
            byte b = pixelProcessor.packPixel(pixel, oldPixelData,pos);
            undo.add(pos.x, pos.y, oldPixelData, getAttr(pos), b, a);
            putPixelData(pos, b, a);
            fireImageChanged(alignX(pos.x), alignY(pos.y), GRID_FACTOR.width, GRID_FACTOR.height);
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
            Point pos = new Point();
            while (Math.round(xi) != x || Math.round(yi) != y) {
                xi += dx;
                yi += dy;
                pos.setLocation(xi, yi);
                setPixel(pixel, pos);
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
            Point pos = new Point(x, y);
            stack.push(pos);
            Pixel pix = getPixel(pos);
            while (!stack.empty()) {
                Point p = stack.pop();
                Pixel pix2 = getPixel(p);
                if (!pix2.hasSameColor(pix, pixelProcessor, palette) || pixels[p.x][p.y]!=null)
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
                    if (pixels[xx][yy] != null) {
                        pos.setLocation(xx,yy);
                        setPixel(pixels[xx][yy],pos);
                    }
                }

            endDraw();
            unlock();
            fireImageChanged();
        }
    }

    public void swap(Pixel pixel, Point pos) {
        if (isInImage(pos)) {
            lock();
            beginDraw();
            int xx = alignX(pos.x);
            int yy = alignY(pos.y);
            Point xy = new Point();
            Pixel old = getPixel(pos);
            for (int i = 0; i < GRID_FACTOR.width; i++) {
                for (int j = 0; j < GRID_FACTOR.height; j++) {
                    xy.setLocation(xx + i, yy + j);
                    Pixel pix = getPixel(xy);
                    if (pix.equals(old))
                        setPixel(pixel, xy);
                    else if (pix.equals(pixel))
                        setPixel(old, xy);
                }
            }
            endDraw();
            unlock();
            fireImageChanged(xx, yy, GRID_FACTOR.width, GRID_FACTOR.height);
        }
    }

    public void copyCell(Point from, Point to) {
        copyCell(this,from, to);
    }


    public void copyCell(Screen source, Point from, Point to) {
        Point f = align(from);
        Point t = align(to);

        if (source.isInImage(f) && isInImage(t)) {
            lock();
            beginDraw();
            byte a = source.getAttr(f);
            Point ff = new Point();
            Point tt = new Point();
            for (int x = 0; x < GRID_FACTOR.width; x++) {
                for (int y = 0; y < GRID_FACTOR.height; y++) {
                    ff.setLocation(f.x + x, f.y + y);
                    tt.setLocation(t.x + x, t.y + y);
                    byte b = source.getPixelData(ff);
                    undo.add(tt.x, tt.y, getPixelData(tt),getAttr(tt),b,a);
                    putPixelData(tt,b, a);
                }
            }
            endDraw();
            unlock();
            fireImageChanged(t.x, t.y, GRID_FACTOR.width, GRID_FACTOR.height);
        }
    }

    public int alignX(int x) {
        return x / GRID_FACTOR.width * GRID_FACTOR.width;
    }

    public int alignY(int y) {
        return y / GRID_FACTOR.height * GRID_FACTOR.height;
    }

    public Point align(Point pos) {
        return new Point(alignX(pos.x), alignY(pos.y));
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

    public void load(InputStream stream, boolean old) throws IOException, ClassNotFoundException {
        ObjectInputStream oStream = new ObjectInputStream(stream);
        if (old) {
            image.load(oStream);
            getPalette().loadPalette(oStream);

        } else {
            getPalette().loadPalette(oStream);
            int x = oStream.readInt();
            int y = oStream.readInt();
            image.load(oStream,x,y);
        }
        oStream.close();
    }

    public abstract void importSCR(InputStream stream) throws IOException;

    public void importImage(DataInputStream is) throws IOException {
        palette.loadPalette(is);
        image.importImage(is);
        is.close();
    }

}

