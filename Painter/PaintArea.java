package Painter;

import Painter.Screen.ImageSupplier;
import Painter.Screen.Pixel;
import Painter.Screen.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.stream.IntStream;

import static javax.swing.SwingUtilities.isMiddleMouseButton;

/**
 * Created by ainozemtsev on 17.11.15.
 * Component for screen editing
 */

public class PaintArea extends JComponent implements Scrollable {
    private Screen screen;
    private int scale = 2;
    private int[] currentColors;
    private ClipCell clipCell;

    public enum Mode { Paint("pencil.png"), Fill("flood.png"), Swap("double_pencil.png");
        private Cursor cursor;

        Mode (String fName) {
            cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                    new ImageIcon(PaintArea.class.getResource(PainterApp.RESOURCE_CURSORS + fName)).getImage(),
                    new Point(2,30),
                    this.name());
        }

        public Cursor getCursor() {
            return cursor;
        }
    }

    private Mode mode = Mode.Paint;

    public PaintArea(Screen screen) {
        super();
        currentColors = new int[screen.getPalette().getTablesCount()];
        setScreen(screen);
        this.clipCell = new ClipCell(screen);
        Listener l = new Listener();
        this.addMouseListener(l);
        this.addMouseMotionListener(l);
        this.addMouseWheelListener(l);
        setCursor(mode.getCursor());
    }

    void setScreen(Screen screen) {
        class ScreenChangeListener implements ImageSupplier.ImageChangeListener {
            @Override
            public void imageChanged(int x, int y, int w, int h) {
                repaint(x*scale,y*scale,w*scale,h*scale);
            }

            @Override
            public void imageChanged() {
                repaint();
            }

            @Override
            public void paletteChanged() {
                repaint();
            }
        }
        this.screen = screen;
        this.updatePreferredSize();
        this.screen.addChangeListener(new ScreenChangeListener());
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle rectangle, int i, int i1) {
        return 2*scale;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle rectangle, int i, int i1) {
        return 16*scale;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void ScrollInView(int x, int y) {
        Rectangle r = new Rectangle((x - 24) * scale, (y - 24) * scale, 48 * scale, 48 * scale);
        PaintArea.this.scrollRectToVisible(r);
    }

    private static final Color gridColor = new Color(128,128,255);
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle c = g.getClipBounds();
        Dimension gf = screen.getGridFactor();

        final int mx = Integer.min((c.x + c.width) / scale, screen.getImageWidth());
        final int my = Integer.min((c.y + c.height) / scale, screen.getImageHeight());
        Point p = new Point();
        for (int x = c.x / scale; x < mx; x++) {
            for (int y = c.y / scale; y < my; y++) {
                p.setLocation(x,y);
                g.setColor(screen.getPixelColor(p));
                int xx = x * scale;
                int yy = y * scale;
                g.fillRect(xx, yy, scale, scale);
                if (scale > 7) {
                    g.setColor(gridColor);
                    if (x % gf.width == 0) g.drawLine(xx, yy, xx, yy + scale - 1);
                    if (y % gf.height == 0) g.drawLine(xx, yy, xx + scale - 1, yy);
                }
            }
        }
    }

    public int getScale() {
        return this.scale;
    }

    public void setScale(int scale) {
        final Rectangle r = getVisibleRect();
        setScale(scale, new Point(r.x + r.width / 2, r.y + r.height / 2));
    }

    public void setScale(int scale, Point fixedPoint) {
        final int s = this.scale;
        Rectangle r = getVisibleRect();
        this.scale = scale;
        updatePreferredSize();
        int xx = fixedPoint.x * (scale - s) / s + r.x;
        int yy = fixedPoint.y * (scale - s) / s + r.y;
        final Rectangle bounds = new Rectangle(0,0, getWidth()-1,getHeight()-1);
        final Rectangle intersection = new Rectangle(xx, yy, r.width, r.height).intersection(bounds);
        scrollRectToVisible(intersection);
        scrollRectToVisible(intersection);
        repaint();
    }

    void updatePreferredSize() {
        final Dimension size = new Dimension(screen.getImageWidth() * scale, screen.getImageHeight() * scale);
        this.setPreferredSize(size);
        this.setSize(size);
        this.revalidate();
    }

    //@Override
    public void colorChanged(int table, int index) {
        currentColors[table] = index;
    }

    public int getCurrentColorIndex(Enum table) {
        return currentColors[table.ordinal()];
    }

    Integer[] getColorIndices() {
        return  IntStream.of(currentColors).boxed().toArray(Integer[]::new);
    }

    public ClipCell getClipCell() {
        return clipCell;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        setCursor(mode.getCursor());
    }

    public Mode getMode() {
        return mode;
    }

    final static String OP_FILL = "fill";
    final static String OP_STATUS = "status";
    final static String OP_SCALE = "scale";
    final static String OP_SWAP = "swap";

    private class Listener implements MouseListener, MouseMotionListener, MouseWheelListener {
        private int button = 0;
        final Point pos = new Point();

        private Pixel getPixelByEvent(int button) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            if ((button & MouseEvent.CTRL_DOWN_MASK) != 0) p += 2;
            final int b = button & MouseEvent.BUTTON1_DOWN_MASK;
            Enum t = screen.mapColorTable((b != 0) ? 0 : 1);
            return new Pixel(t, getCurrentColorIndex(t), p);
        }

        private boolean isInSameCell(Point p) {
            Dimension gf = screen.getGridFactor();
            int fx = scale * gf.width;
            int fy = scale * gf.height;
            return (pos.x/fx == p.x/fx) && (pos.y/fy == p.y/fy);
        }



        @Override
        public void mouseClicked(MouseEvent e) {
            int button = e.getModifiersEx();
            if (e.getButton() == MouseEvent.BUTTON1) button = button | MouseEvent.BUTTON1_DOWN_MASK;
            Point pos = new Point(e.getX()/scale, e.getY()/scale);
            if (isFillMode(e.getButton())) {
                final Pixel pixel = getPixelByEvent(button);
                screen.fill(e.getX()/scale,e.getY()/scale, pixel);
                firePropertyChange(OP_FILL,"0","1");
                return;
            }
            if (isSwapMode(e.getButton())) {
                final Pixel pixel = getPixelByEvent(button);
                screen.swap(pixel, pos);
                firePropertyChange(OP_SWAP,"0","1");
                return;
            }
            if (isMiddleMouseButton(e)) {
                final int xx = e.getX() / scale ;
                final int yy = e.getY() / scale ;
                if ((button & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                    clipCell.setLocation(screen, xx, yy);
                    clipCell.repaint();
                }
            }
        }

        boolean isFillMode(final int button) {
            return mode == Mode.Fill && (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3);
        }

        boolean isSwapMode(final int button) {
            return mode == Mode.Swap && (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3);
        }


        @Override
        public void mousePressed(MouseEvent e) {
            pos.setLocation(e.getPoint());
            final int btn = e.getButton();
            if (isFillMode(btn) || isSwapMode(btn)) return;
            screen.beginDraw();
            if (isMiddleMouseButton(e)) {
                if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0) {
                    final Point p = new Point(e.getX() / scale, e.getY() / scale);
                    screen.copyCell(clipCell.getScreen(), clipCell.getPosition(),p);
                }
            } else {
                this.button = e.getModifiersEx()
                        & (MouseEvent.SHIFT_DOWN_MASK
                        | MouseEvent.CTRL_DOWN_MASK
                        | MouseEvent.BUTTON1_DOWN_MASK
                        | MouseEvent.BUTTON3_DOWN_MASK);
                doSetPixel(e);
            }
        }

        private void doSetPixel(MouseEvent e) {
            Pixel pixel = getPixelByEvent(button);
            Point p = new Point(e.getX()/scale, e.getY()/scale);
            screen.setPixel(pixel,p);
        }

        private void doDrawLine(MouseEvent e) {
            Pixel pixel = getPixelByEvent(button);
            screen.drawLine(pos.x/scale, pos.y/scale, e.getX()/scale, e.getY()/scale, pixel);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mode != Mode.Paint) return;
            if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0 && isMiddleMouseButton(e)) {
                if (!isInSameCell(e.getPoint())) {
                    Point toPos = new Point(e.getX() / scale, e.getY() / scale);
                    screen.copyCell(clipCell.getScreen(), clipCell.getPosition(), toPos);
                }
            } else if (!pos.equals(e.getPoint())) doDrawLine(e);
            pos.setLocation(e.getPoint());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            screen.endDraw();
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point p = new Point( e.getX() / scale, e.getY() / scale);
            if (screen.isInImage(p)) {
                Pixel s = screen.getPixel(p);
                firePropertyChange(OP_STATUS, "", p.x + "x" + p.y + " : " + s.table.name()+s.shift+" = "+s.index);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
                final int s = PaintArea.this.scale;
                if (e.getWheelRotation()>0 && s >1)  setScale(s -1, e.getPoint());
                if (e.getWheelRotation()<0 && s <16) setScale(s +1, e.getPoint());
                firePropertyChange(OP_SCALE,s,scale);
                mouseMoved(e);
            } else getParent().dispatchEvent(e);
        }
    }

}
