package Painter.Palette;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ainozemtsev on 28.01.16.
 */
public class PaletteToolBar extends JToolBar {
    private final JToolBar inkBar = new JToolBar();
    private final JToolBar paperBar = new JToolBar();
    Palette palette;

    public PaletteToolBar(Palette palette) {
        super();
        this.palette = palette;
        setFloatable(false);
        inkBar.setFloatable(false);
        this.createButtonGroup(Palette.Table.INK, inkBar);
        add(inkBar);
        paperBar.setFloatable(false);
        this.createButtonGroup(Palette.Table.PAPER, paperBar);
        add(paperBar);
    }

    ButtonGroup createButtonGroup(Palette.Table table, JToolBar toolBar) {
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 8; i++) {
            PaletteButton b = new PaletteButton(palette, table, i);
            if (i == 0) b.setSelected(true);
            b.addActionListener(e -> fireColorChange(b.getTable(), b.getIndex()));
            group.add(b);
            toolBar.add(b);
        }
        JToggleButton transparentButton = new JToggleButton("T");
        transparentButton.addActionListener(e -> fireColorChange(table, 8));
        group.add(transparentButton);
        toolBar.add(transparentButton);
        toolBar.addSeparator();
        return group;
    }

    public interface ColorChangeListener {
        void colorChanged(Palette.Table table, int index);
    }

    List<ColorChangeListener> listeners = new ArrayList<>();

    public void addActionListener(ColorChangeListener listener) {
        listeners.add(listener);
    }

    private void fireColorChange(Palette.Table table, int index) {
        listeners.forEach(listener -> listener.colorChanged(table, index));
    }

}
