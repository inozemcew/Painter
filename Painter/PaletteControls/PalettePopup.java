package Painter.PaletteControls;

import Painter.Screen.Palette.Palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by ainozemtsev on 26.11.15.
 * Popup menu for selection colors in palette.
 */
public class PalettePopup extends JPopupMenu {

    public interface ColorIndexSupplier {
        int getColorCell();
        void setColorCell(int index);
    }

    MouseListener listener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            doPopUp(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            doPopUp(e);
        }
    };

    private static Map<Integer, PalettePopup> palettePopups = new HashMap<>();

    public static MouseListener createPalettePopup(Palette palette, int table) {
        return palettePopups.computeIfAbsent(table, k -> new PalettePopup(palette.getCellSize(table))).listener;
    }

    private ColorIndexSupplier button = null;
    private ButtonGroup[] groups;


    private PalettePopup(int cellSize) {
        super();
        groups = new ButtonGroup[cellSize];
        this.setLayout(new GridLayout(5*groups.length-1, 16));
        for (int i = 0; i < groups.length; i++) {
            groups[i] = createButtonGroup();
            if (i == groups.length-1) break;
            IntStream.range(0,16).forEach(x -> addSeparator());
        }
    }

    private ButtonGroup createButtonGroup() {
        ButtonGroup g1 = new ButtonGroup();
        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 16; i++) {
                JRadioButtonMenuItem b = new JRadioButtonMenuItem(new PopupIcon(i + 16 * j));
                b.setSelectedIcon(null);
                b.addActionListener(this::doSelect);
                this.add(b);
                g1.add(b);
            }
        return g1;
    }

    private void doPopUp (MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.button = (ColorIndexSupplier) e.getComponent();
            int colorIndex = button.getColorCell();
            for (int i =0; i< groups.length; i++) {
                JMenuItem m = (JMenuItem)getSubElements()[Palette.split(colorIndex,i)+64*i];
                m.setSelected(true);
            }
            show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void doSelect(ActionEvent e) {
        button.setColorCell(Palette.combine(
                IntStream.range(0,groups.length)
                .map(i -> findSelected(groups[i]))
                .toArray()
        ));
    }

    private int findSelected(ButtonGroup group) {
        int i =0;
        for (Enumeration<AbstractButton> bs = group.getElements(); bs.hasMoreElements(); i++)
            if (bs.nextElement().isSelected())
                return i;
        return -1;
    }

    private class PopupIcon implements Icon {
        public static final int size = 16;
        private final int index;

        public PopupIcon(int i) {
            this.index = i;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Palette.toRGB(index));
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }



}
