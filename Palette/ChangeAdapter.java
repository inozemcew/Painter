package Painter.Palette;

import Painter.Screen;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by ainozemtsev on 31.03.16.
 */
public class ChangeAdapter extends AbstractAction implements PaletteToolPanel.ChangeListener {
    int ink = 0, paper = 0;
    private boolean selected = false;
    private Screen screen;

    public ChangeAdapter(Screen screen) {
        super("Enhance");
        this.screen = screen;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source instanceof JToggleButton ) {
            selected = ((JToggleButton) source).isSelected();
            if (selected)
                screen.setEnhanced(ink, paper);
            else
                screen.setEnhanced(-1, -1);
        }
    }

    @Override
    public void colorChanged(Palette.Table table, int index) {
        if (table == Palette.Table.INK) ink = index; else paper = index;
        if (selected) screen.setEnhanced(ink, paper);
    }

    @Override
    public void reorder(Palette.Table table, int from, int to) {
        screen.swapColors(table, from, to);
    }
}
