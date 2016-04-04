package Painter.Palette;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ainozemtsev on 28.01.16.
 */
public class PaletteToolPanel extends JPanel {
    Palette palette;
    private int orientation = JToolBar.HORIZONTAL;

    public PaletteToolPanel(Palette palette) {
        super();
        setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
        this.palette = palette;
        this.createButtonGroup(Palette.Table.INK);
        this.createButtonGroup(Palette.Table.PAPER);
    }

    @Override
    public Dimension getPreferredSize() {
        if ( orientation == JToolBar.HORIZONTAL) return new Dimension(900,48);
        else return new Dimension(96,500);
    }

    ButtonGroup createButtonGroup(Palette.Table table) {
        ButtonGroup group = new ButtonGroup();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.LINE_AXIS));
        p.setMinimumSize(new Dimension(50,50));
        p.setMaximumSize(new Dimension(380,380));
        p.setOpaque(false);
        final DragListener l = new DragListener(p,(f, t) -> fireReorder(table,f,t));
        for (int i = 0; i < 8; i++) {
            PaletteButton b = new PaletteButton(palette, table, i);
            if (i == 0) b.setSelected(true);
            l.addComponent(b);
            b.addActionListener(e -> fireColorChange(b.getTable(), b.getIndex()));
            group.add(b);
            p.add(b);
        }
        JToggleButton transparentButton = new JToggleButton("T");
        transparentButton.addActionListener(e -> fireColorChange(table, -1));
        group.add(transparentButton);
        p.add(transparentButton);

        this.add(p);
        this.add(Box.createRigidArea(new Dimension(10,10)));
        return group;
    }

    public interface ChangeListener {
        void colorChanged(Palette.Table table, int index);
        void reorder(Palette.Table table, int from, int to);
    }

    List<ChangeListener> listeners = new ArrayList<>();

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    private void fireColorChange(Palette.Table table, int index) {
        listeners.forEach(listener -> listener.colorChanged(table, index));
    }

    private void fireReorder(Palette.Table table, Integer from, Integer to) {
        listeners.forEach(listener -> listener.reorder(table, from, to));
    }

    public void setOrientation(int o) {
        if (this.orientation == o) return;
        this.orientation = o;
        for (Component c : getComponents()) {
            if (c instanceof JPanel) {
                final LayoutManager l;
                if (o == JToolBar.HORIZONTAL)
                    l = new BoxLayout((JPanel) c, BoxLayout.LINE_AXIS);
                else l = new FlowLayout(FlowLayout.LEFT, 0, 0);
                ((JPanel) c).setLayout(l);
            }
        }
        setLayout(new BoxLayout(this, (o == JToolBar.HORIZONTAL) ? BoxLayout.LINE_AXIS : BoxLayout.PAGE_AXIS));
        revalidate();
    }

}
