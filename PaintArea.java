package Painter;

import Painter.Palette.Palette;
import Painter.Palette.PaletteButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by ainozemtsev on 17.11.15.
 */
public class PaintArea extends JComponent implements Scrollable {
    private Screen screen;
    private int scale = 2;
    private final JToolBar inkBar = new JToolBar();
    private final JToolBar paperBar = new JToolBar();

    public PaintArea(Screen screen) {
        super();
        this.screen = screen;
        this.updatePreferredSize();
        Listener l = new Listener();
        this.addMouseListener(l);
        this.addMouseMotionListener(l);
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

    public JComponent createToolBar() {
        JComponent panel = new JToolBar() {{
            setFloatable(false);
        }};
        inkBar.setFloatable(false);
        this.createButtonGroup(Palette.Table.INK, inkBar);
        panel.add(inkBar);
        paperBar.setFloatable(false);
        this.createButtonGroup(Palette.Table.PAPER, paperBar);
        panel.add(paperBar);
        return panel;
    }

    ButtonGroup createButtonGroup(Palette.Table table, JToolBar toolBar) {
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 8; i++) {
            AbstractButton b = new PaletteButton(screen.getPalette(), table, i);
            if (i == 0) b.setSelected(true);
            group.add(b);
            toolBar.add(b);
        }
        JToggleButton transparentButton = new JToggleButton("T");
        group.add(transparentButton);
        toolBar.add(transparentButton);
        toolBar.addSeparator();
        return group;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int x = 0; x < screen.getImageWidth(); x++)
            for (int y = 0; y < screen.getImageHeight(); y++) {
                g.setColor(screen.getPixelColor(x,y));
                int xx = x * scale;
                int yy = y * scale;
                g.fillRect(xx, yy, scale, scale);
                if (scale > 7) {
                    g.setColor(Color.PINK);
                    if (x % 8 == 0) g.drawLine(xx, yy, xx, yy + scale - 1);
                    if (y % 8 == 0) g.drawLine(xx, yy, xx + scale - 1, yy);
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
        this.setPreferredSize(new Dimension(ImageBuffer.SIZE_X * scale, ImageBuffer.SIZE_Y * scale));
        this.revalidate();
    }

    private int findColorIndex(Palette.Table table) {
        JToolBar group = (table == Palette.Table.INK) ? inkBar : paperBar;
        for (int i = 0; i < group.getComponentCount(); i++) {
            Component component = group.getComponent(i);
            if (component instanceof PaletteButton && ((PaletteButton) component).isSelected())
                return ((PaletteButton) component).getIndex();
        }
        return -1;
    }

    class Listener implements MouseListener, MouseMotionListener, PropertyChangeListener {
        private int button = 0;
        Point pos = new Point();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("orientation")) {
                inkBar.setOrientation((Integer) evt.getNewValue());
                paperBar.setOrientation((Integer) evt.getNewValue());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int button = e.getModifiersEx();
            if ((button & MouseEvent.CTRL_DOWN_MASK) != 0 ) {
                byte p;
                if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
                Palette.Table t = (e.getButton() == MouseEvent.BUTTON1) ? Palette.Table.INK : Palette.Table.PAPER;
                screen.fill(e.getX()/scale,e.getY()/scale,t,findColorIndex(t),p);
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
            screen.setPixel(e.getX()/scale, e.getY()/scale, t, findColorIndex(t), p);
        }

        private void doDrawLine(MouseEvent e) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            Palette.Table t = (button & MouseEvent.BUTTON1_DOWN_MASK) != 0 ? Palette.Table.INK : Palette.Table.PAPER;
            screen.drawLine(pos.x/scale, pos.y/scale, e.getX()/scale, e.getY()/scale,
                    t,findColorIndex(t), p);
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
                String s = screen.getPixelDescription(x,y);
                firePropertyChange("status", "", x + "x" + y + " : " + s);
            }
        }
    }
}

