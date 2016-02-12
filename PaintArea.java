package Painter;

import Painter.Palette.Palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Created by ainozemtsev on 17.11.15.
 * Component for screen editing
 */
public class PaintArea extends JComponent implements Scrollable {
    private Screen screen;
    private int scale = 2;
    private int[] ink_paper = {0,0};

    public PaintArea(Screen screen) {
        super();
        setScreen(screen);
        Listener l = new Listener();
        this.addMouseListener(l);
        this.addMouseMotionListener(l);
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
                    if (x % 8 == 0) g.drawLine(xx, yy, xx, yy + scale - 1);
                    if (y % 8 == 0) g.drawLine(xx, yy, xx + scale - 1, yy);
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

    private void updatePreferredSize() {
        this.setPreferredSize(new Dimension(screen.getImageWidth() * scale, screen.getImageHeight() * scale));
        this.revalidate();
    }

    //@Override
    public void colorChanged(Palette.Table table, int index) {
        ink_paper[table.ordinal()] = index;
    }

    int getColorIndex(Palette.Table table) {
        return ink_paper[table.ordinal()];
    }

    class Listener implements MouseListener, MouseMotionListener {
        private int button = 0;
        Point pos = new Point();

        /*@Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("orientation")) {
                inkBar.setOrientation((Integer) evt.getNewValue());
                paperBar.setOrientation((Integer) evt.getNewValue());
            }
        }*/

        @Override
        public void mouseClicked(MouseEvent e) {
            int button = e.getModifiersEx();
            if ((button & MouseEvent.CTRL_DOWN_MASK) != 0 ) {
                byte p;
                if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
                Palette.Table t = (e.getButton() == MouseEvent.BUTTON1) ? Palette.Table.INK : Palette.Table.PAPER;
                screen.fill(e.getX()/scale,e.getY()/scale,t, getColorIndex(t),p);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pos.setLocation(e.getPoint());
            if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) return;
            button = e.getModifiersEx()
                    & (MouseEvent.SHIFT_DOWN_MASK
                    | MouseEvent.BUTTON1_DOWN_MASK
                    | MouseEvent.BUTTON2_DOWN_MASK);
            screen.beginDraw();
            doSetPixel(e);
        }

        private void doSetPixel(MouseEvent e) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            Palette.Table t = (button & MouseEvent.BUTTON1_DOWN_MASK) != 0 ? Palette.Table.INK : Palette.Table.PAPER;
            screen.setPixel(e.getX()/scale, e.getY()/scale, t, getColorIndex(t), p);
        }

        private void doDrawLine(MouseEvent e) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            Palette.Table t = (button & MouseEvent.BUTTON1_DOWN_MASK) != 0 ? Palette.Table.INK : Palette.Table.PAPER;
            screen.drawLine(pos.x/scale, pos.y/scale, e.getX()/scale, e.getY()/scale,
                    t, getColorIndex(t), p);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) return;
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
                Palette.PixelDescriptor s = screen.getPixelDescriptor(x,y);
                firePropertyChange("status", "", x + "x" + y + " : " + s.table.name()+s.shift+" = "+s.index);
            }
        }
    }
}

