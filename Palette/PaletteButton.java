package Painter.Palette;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ainozemtsev on 26.11.15.
 */
public class PaletteButton extends JToggleButton implements Palette.PaletteChangeListener, PalettePopup.ColorIndexSupplier{
    private final Palette palette;
    private final Palette.Table table;
    private final int index;

    public PaletteButton(Palette palette, Palette.Table table, int index) {
        super();
        this.index = index;
        this.table = table;
        this.palette = palette;
        setIcon(new PaletteIcon(palette.getColor(table, index, 0), palette.getColor(table, index, 1)));
        setMinimumSize(new Dimension(32, 32));
        setFocusPainted(false);
        addMouseListener(PalettePopup.createPalettePopup());
        palette.addChangeListener(this, table, index);
    }

    public int getColorIndex() {
        return palette.getColorIndex(this.table, this.index);
    }

    public int getIndex() {
        return this.index;
    }

    public void setColorIndex(int value) {
        palette.setColorIndex(value,this.table,this.index);
    }

    @Override
    public void paletteChanged() {
        setIcon(new PaletteIcon(palette.getColor(table, index, 0), palette.getColor(table, index, 1)));
    }
}

class PaletteIcon implements Icon {
    private final int SIZE = 32;
    private Color color1, color2;

    public PaletteIcon(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(this.color1);
        g.fillRect(x, y, SIZE, SIZE / 2);
        g.setColor(this.color2);
        g.fillRect(x, SIZE / 2 + y, SIZE, SIZE / 2);
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
