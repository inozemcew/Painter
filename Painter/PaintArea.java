package Painter;

import Painter.Screen.ImageSupplier;
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
    public enum Mode {Paint, Fill}

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
        for (int x = c.x / scale; x < mx; x++) {
            for (int y = c.y / scale; y < my; y++) {
                g.setColor(screen.getPixelColor(x,y));
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
        this.scale = scale;
        updatePreferredSize();
        repaint();
    }

    void updatePreferredSize() {
        this.setPreferredSize(new Dimension(screen.getImageWidth() * scale, screen.getImageHeight() * scale));
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
    }

    public Mode getMode() {
        return mode;
    }

    final static String OP_FILL = "fill";
    final static String OP_STATUS = "status";
    final static String OP_SCALE = "scale";

    private class Listener implements MouseListener, MouseMotionListener, MouseWheelListener {
        private int button = 0;
        final Point pos = new Point();

        private Screen.Pixel getPixelByEvent(int button) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            if ((button & MouseEvent.CTRL_DOWN_MASK) != 0) p += 2;
            final int b = button & MouseEvent.BUTTON1_DOWN_MASK;
            Enum t = screen.mapColorTable((b != 0) ? 0 : 1);
            return new Screen.Pixel(t, getCurrentColorIndex(t), p);
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
            if (isFillMode(e.getButton())) {
                final Screen.Pixel pixel = getPixelByEvent(button);
                screen.fill(e.getX()/scale,e.getY()/scale, pixel);
                firePropertyChange(OP_FILL,"0","1");
            } else if (isMiddleMouseButton(e)) {
                final int xx = e.getX() / scale ;
                final int yy = e.getY() / scale ;
                if ((button & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                    clipCell.setLocation(screen, xx, yy);
                    clipCell.repaint();
                }
            }
        }

        boolean isFillMode(int button) {
            return mode == Mode.Fill && (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pos.setLocation(e.getPoint());
            if (isFillMode(e.getButton())) return;
            screen.beginDraw();
            if (isMiddleMouseButton(e)) {
                if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0) {
                    final int xx = e.getX() / scale;
                    final int yy = e.getY() / scale;
                    screen.copyCell(clipCell.getScreen(), clipCell.getX(), clipCell.getY(), xx, yy);
                }
            } else {
                button = e.getModifiersEx()
                        & (MouseEvent.SHIFT_DOWN_MASK
                        | MouseEvent.CTRL_DOWN_MASK
                        | MouseEvent.BUTTON1_DOWN_MASK
                        | MouseEvent.BUTTON3_DOWN_MASK);
                doSetPixel(e);
            }
        }

        private void doSetPixel(MouseEvent e) {
            Screen.Pixel pixel = getPixelByEvent(button);
            screen.setPixel(e.getX()/scale, e.getY()/scale, pixel);
        }

        private void doDrawLine(MouseEvent e) {
            Screen.Pixel pixel = getPixelByEvent(button);
            screen.drawLine(pos.x/scale, pos.y/scale, e.getX()/scale, e.getY()/scale, pixel);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0 && isMiddleMouseButton(e)) {
                if (!isInSameCell(e.getPoint()))
                    screen.copyCell(clipCell.getScreen(), clipCell.getX(), clipCell.getY(), e.getX() / scale, e.getY() / scale);
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
            int x = e.getX() / scale;
            int y = e.getY() / scale;
            if (screen.isInImage(x,y)) {
                Screen.Pixel s = screen.getPixelDescriptor(x,y);
                firePropertyChange(OP_STATUS, "", x + "x" + y + " : " + s.table.name()+s.shift+" = "+s.index);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
                final int s = PaintArea.this.scale;
                if (e.getWheelRotation()>0 && s >1) setScale(s -1);
                if (e.getWheelRotation()<0 && s <16) setScale(s +1);
                firePropertyChange(OP_SCALE,s,scale);
            } else getParent().dispatchEvent(e);
        }
    }

}

