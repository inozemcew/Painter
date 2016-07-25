package Painter;

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
        this.screen = screen;
        this.updatePreferredSize();
        this.screen.addChangeListener(new ImageSupplier.ImageChangeListener() {
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
        });
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

    private static Color gridColor = new Color(128,128,255);
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle c = g.getClipBounds();

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
                    if (x % screen.GRID_FACTOR_X == 0) g.drawLine(xx, yy, xx, yy + scale - 1);
                    if (y % screen.GRID_FACTOR_Y == 0) g.drawLine(xx, yy, xx + scale - 1, yy);
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

    public int getColorIndex(Enum table) {
        return currentColors[table.ordinal()];
    }

    Integer[] getColorIndices() {
        return  IntStream.of(currentColors).boxed().toArray(Integer[]::new);
    }

    public ClipCell getClipCell() {
        return clipCell;
    }

    class Listener implements MouseListener, MouseMotionListener, MouseWheelListener {
        private int button = 0;
        Point pos = new Point();

        private Screen.Pixel getPixelByEvent(int button) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            final int b = button & MouseEvent.BUTTON1_DOWN_MASK;
            Enum t = screen.mapColorTable((b != 0) ? 0 : 1);
            return new Screen.Pixel(t, getColorIndex(t), p);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int button = e.getModifiersEx();
            if (e.getButton() == MouseEvent.BUTTON1) button = button | MouseEvent.BUTTON1_DOWN_MASK;
            if ((button & MouseEvent.CTRL_DOWN_MASK) != 0 ) {
                final Screen.Pixel pixel = getPixelByEvent(button);
                screen.fill(e.getX()/scale,e.getY()/scale, pixel);
            } else if (isMiddleMouseButton(e)) {
                final int xx = e.getX() / scale ;
                final int yy = e.getY() / scale ;
                if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) {
                    screen.copyCell(clipCell.getScreen(),clipCell.getX(),clipCell.getY(), xx, yy);
                } else {
                    clipCell.setLocation(screen, xx, yy);
                    clipCell.repaint();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pos.setLocation(e.getPoint());
            if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0 || isMiddleMouseButton(e) ) return;
            button = e.getModifiersEx()
                    & (MouseEvent.SHIFT_DOWN_MASK
                    | MouseEvent.BUTTON1_DOWN_MASK
                    | MouseEvent.BUTTON3_DOWN_MASK);
            screen.beginDraw();
            doSetPixel(e);
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
            if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) return;
            if (e.getModifiersEx() == 0 && isMiddleMouseButton(e))
                screen.copyCell(clipCell.getX(), clipCell.getY(), e.getX() / scale , e.getY() / scale );
            if (pos.equals(e.getPoint())) return;
            doDrawLine(e);
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
                firePropertyChange("status", "", x + "x" + y + " : " + s.table.name()+s.shift+" = "+s.index);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isControlDown()) {
                final int s = PaintArea.this.scale;
                if (e.getWheelRotation()>0 && s >1) setScale(s -1);
                if (e.getWheelRotation()<0 && s <16) setScale(s +1);
                getRootPane().firePropertyChange("scale",s,scale);
            } else getParent().dispatchEvent(e);
        }
    }
}

