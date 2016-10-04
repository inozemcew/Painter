package Painter.PaletteControls;

import Painter.Screen.Screen;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by ainozemtsev on 31.03.16.
 * Abstract adapter for palette toolbar events
 */
public class ChangeAdapter implements PaletteToolPanel.ChangeListener {
    private int[] colors;
    private boolean selected = false;
    private Screen screen = null;

    public ChangeAdapter() {

    }

    public ChangeAdapter(Screen screen) {
        this();
        setScreen(screen);
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
        colors = new int[screen.getPalette().getTablesCount()];
    }

    public Action createAction() {
        return new AbstractAction("Enhance") {
            {
                putValue(SELECTED_KEY, false);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                setSelected(Boolean.TRUE.equals(getValue(SELECTED_KEY)));
            }
        };
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (Boolean.TRUE.equals(selected))
            screen.setEnhanced(colors);
        else
            screen.resetEnhanced();
    }

    @Override
    public void colorChanged(int table, int index) {
        colors[table] = index;
        if (selected)
            screen.setEnhanced(colors);
    }

    @Override
    public void reorder(int table, int from, int to) {
        screen.swapColors(table, from, to);
    }

}
