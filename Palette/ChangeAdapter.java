package Painter.Palette;

import Painter.Screen;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by ainozemtsev on 31.03.16.
 */
public class ChangeAdapter extends AbstractAction implements PaletteToolPanel.ChangeListener {
    int[] colors;
    private boolean selected = false;
    private Screen screen;

    public ChangeAdapter(Screen screen) {
        super("Enhance");
        this.screen = screen;
        colors = new int[screen.getPalette().getTablesCount()];
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source instanceof JToggleButton ) {
            selected = ((JToggleButton) source).isSelected();
            if (selected)
                screen.setEnhanced(colors);
            else
                screen.resetEnhanced();
        }
    }

    @Override
    public void colorChanged(int table, int index) {
        colors[table] = index;
        if (selected) screen.setEnhanced(colors);
    }

    @Override
    public void reorder(int table, int from, int to) {
        screen.swapColors(table, from, to);
    }
}
