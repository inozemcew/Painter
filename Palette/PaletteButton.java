package Painter.Palette;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ainozemtsev on 26.11.15.
 * Button for selection current colors
 */
public class PaletteButton extends JToggleButton
        implements Palette.PaletteChangeListener, PalettePopup.ColorIndexSupplier {
    private final Palette palette;
    private final Palette.Table table;
    private final int index;

    public PaletteButton(Palette palette, Palette.Table table, int index) {
        super();
        this.index = index;
        this.table = table;
        this.palette = palette;
        setIcon(new PaletteIcon(palette.getColorCell(table, index)));
        setMinimumSize(new Dimension(32, 32));
        setPreferredSize(new Dimension(42,42));
        setFocusPainted(false);
        addMouseListener(PalettePopup.createPalettePopup());
        palette.addChangeListener(this, table, index);
    }

    public int getColorCell() {
        return palette.getColorCell(this.table, this.index);
    }

    public int getIndex() {
        return this.index;
    }

    public Palette.Table getTable() {
        return this.table;
    }

    public void setColorCell(int value) {
        palette.setColorCell(value,this.table,this.index);
    }

    @Override
    public void paletteChanged() {
        setIcon(new PaletteIcon(palette.getColorCell(table, index)));
    }

/*    @Override
    public void reorder(int oldIndex, int newIndex) {

    }*/
}

class PaletteIcon implements Icon {
    private final int SIZE = 32;
    private Color[] colors = new Color[Palette.COLORS_PER_CELL];

    public PaletteIcon(int cell) {
        for (int i = 0; i < colors.length; i++) {
            this.colors[i] = Palette.toRGB(Palette.split(cell,i));
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        int dy = SIZE/colors.length;
        for (int i=0; i < colors.length; i++) {
            g.setColor(this.colors[i]);
            g.fillRect(x, y + dy * i, SIZE , dy);

        }
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}

