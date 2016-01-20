package Painter;

import Painter.Palette.Palette;
import Painter.Palette.Palette2;
import Painter.Palette.PaletteButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

/**
 * Created by ainozemtsev on 17.11.15.
 */
public class PaintArea extends JComponent {
    private PaintImage image = new PaintImage();
    private int scale = 2;
    private Palette palette = new Palette();
    private final JToolBar inkBar = new JToolBar();
    private final JToolBar paperBar = new JToolBar();
    private JComponent interlacedView = null;


    public PaintArea() {
        super();
        this.updatePreferredSize();
        Listener l = new Listener();
        this.addMouseListener(l);
        this.addMouseMotionListener(l);
        palette.addChangeListener(this::repaint);
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
        //JToggleButton transparentButton = new JToggleButton("T");
        //transparentButton.setAction();
        //panel.add(transparentButton);
        return panel;
    }

    ButtonGroup createButtonGroup(Palette.Table table, JToolBar toolBar) {
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 8; i++) {
            AbstractButton b = new PaletteButton(palette, table, i);
            if (i == 0/*palette.getCurrentColorIndex(table)*/) b.setSelected(true);
            group.add(b);
            toolBar.add(b);
        }
        toolBar.addSeparator();
        JToggleButton transparentButton = new JToggleButton("T");
        group.add(transparentButton);
        toolBar.add(transparentButton);
        return group;
    }

    public JComponent createInterlacedView() {
        this.interlacedView = new JComponent() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                image.paintInterlaced(g, scale, PaintArea.this.palette);
            }
        };
        this.interlacedView.setPreferredSize(new Dimension(512, 384));
        palette.addChangeListener(this.interlacedView::repaint);
        this.interlacedView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Rectangle r = new Rectangle((e.getX() / 2 - 24) * scale, (e.getY() / 2 - 24) * scale, 48 * scale, 48 * scale);
                PaintArea.this.scrollRectToVisible(r);
            }
        });
        return this.interlacedView;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        image.paint(g, scale, this.palette);
        //g.drawImage(image,0,0,256*scale, 192*scale, null);
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
        this.setPreferredSize(new Dimension(256 * scale, 192 * scale));
        this.revalidate();
    }

    private boolean isInImage(Point p) {
        return isInImage(p.x, p.y);
    }

    private boolean isInImage(int x, int y) {
        return x >= 0 && x < 256 * scale && y >= 0 && y < 192 * scale;
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

    private void setPixel(int x, int y, Palette.Table table, byte shift) {
        int xx = x / scale;
        int yy = y / scale;
        if (isInImage(xx, yy)) {
            image.setPixel(xx,yy,table, findColorIndex(table), shift);
            repaint(xx / 8 * 8 * scale, yy / 8 * 8 * scale, scale * 8, scale * 8);
            if (interlacedView != null) interlacedView.repaint(xx / 8 * 16, yy / 8 * 16, 16, 16);
        }
    }

    private void drawLine(int ox, int oy, int x, int y, Palette.Table table, byte shift) {
        int xx = x / scale, yy = y / scale;
        int oxx = ox / scale, oyy = oy / scale;
        if (isInImage(xx, yy) && isInImage(ox, oy)) {
            image.drawLine(oxx, oyy, xx, yy, table, findColorIndex(table), shift);
            int dx, dy, w, h ;
            if (ox < x) {
                dx = oxx / 8 * 8;
                w =  xx / 8 * 8 + 8 - dx;
            } else {
                dx = xx / 8 * 8;
                w = oxx / 8 * 8 + 8 - dx;
            }
            if (oy < y) {
                dy = oyy / 8 * 8;
                h =  yy / 8 * 8 + 8 - dy;
            } else {
                dy = yy / 8 * 8;
                h = oyy / 8 * 8 + 8 - dy;
            }
            repaint(dx * scale, dy * scale, scale * w, scale * h);
            //System.out.println(ox+","+x+","+dx+","+w+" - "+oy+","+y+","+dy+","+h);
            if (interlacedView != null) interlacedView.repaint(dx * 2, dy * 2, w*2, h*2);
        }
    }

    private void fill(int x, int y, Palette.Table table, byte shift) {
        int xx = x / scale;
        int yy = y / scale;
        if (isInImage(xx, yy)) {
            image.beginDraw();
            image.fill(xx, yy, table, findColorIndex(table), shift);
            image.endDraw();
            repaint();
            if (interlacedView != null) interlacedView.repaint();
        }
    }


    public void undo() {
        image.undoDraw();
        getRootPane().repaint();
    }

    public void redo() {
        image.redoDraw();
        getRootPane().repaint();
    }

    public void importSCR(File file) throws IOException {
        final FileInputStream stream = new FileInputStream(file);
        image.importSCR(stream);
        int[] ink = {0x20, 0x24, 0x28, 0x2e, 0x2c, 0x2a, 0x26, 0x22};
        for (int i = 0; i < 8; i++) ink[i] += (ink[i] + 0x10) << 6;
        palette.setPalette(ink, ink);
        stream.close();
        repaint();
    }

    public void importPNG(File file) throws IOException {
        final FileInputStream stream = new FileInputStream(file);
        int[][] p = image.importPNG(stream);
        stream.close();
        palette.setPalette(p[0],p[1]);
        repaint();
    }

    public void save(File file) throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
        image.save(stream);
        stream.writeObject(palette.getPalette(Palette.Table.INK));
        stream.writeObject(palette.getPalette(Palette.Table.PAPER));
        stream.close();
    }

    public void load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
        image.load(stream);
        int[] ink = (int[]) stream.readObject();
        int[] paper = (int[]) stream.readObject();
        palette.setPalette(ink,paper);
        stream.close();
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
                fill(e.getX(),e.getY(),t,p);
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
            image.beginDraw();
            doSetPixel(e);
        }

        private void doSetPixel(MouseEvent e) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            Palette.Table t = (button & MouseEvent.BUTTON1_DOWN_MASK) != 0 ? Palette.Table.INK : Palette.Table.PAPER;
            setPixel(e.getX(), e.getY(), t, p);
        }

        private void doDrawLine(MouseEvent e) {
            byte p;
            if ((button & MouseEvent.SHIFT_DOWN_MASK) == 0) p = 0; else p = 1;
            Palette.Table t = (button & MouseEvent.BUTTON1_DOWN_MASK) != 0 ? Palette.Table.INK : Palette.Table.PAPER;
            drawLine(pos.x, pos.y, e.getX(), e.getY(), t, p);
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
            image.endDraw();
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (isInImage(e.getPoint())) {
                int x = e.getX() / scale;
                int y = e.getY() / scale;
                int v = image.getPixel(x, y);
                String s = ((v < 2) ? "Paper" : "Ink") + String.valueOf(v & 1) + "=" + String.valueOf(image.getAttr(x, y));
                firePropertyChange("status", "", x + "x" + y + " : " + s);
            }
        }
    }
}

