package Painter.Palette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

/**
 * Created by ainozemtsev on 26.11.15.
 */
public class PalettePopup extends JPopupMenu {

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

    private static PalettePopup palettePopup = new PalettePopup();

    public static MouseListener createPalettePopup(PaletteButton button) {
        return palettePopup.listener;
    }

    private PaletteButton button = null;
    private ButtonGroup group1,group2;


    private PalettePopup() {
        super();
        this.setLayout(new GridLayout(9, 16));
        group1 = createButtonGroup();
        for (int i = 0; i < 16; i++) addSeparator();
        group2 = createButtonGroup();
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
            this.button = (PaletteButton) e.getComponent();
            int colorIndex = button.getColorIndex();
            JMenuItem m1 = (JMenuItem)getSubElements()[Palette.first(colorIndex)];
            JMenuItem m2 = (JMenuItem)getSubElements()[Palette.second(colorIndex)+64];
            m1.setSelected(true);
            m2.setSelected(true);
            show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void doSelect(ActionEvent e) {
        button.setColorIndex(Palette.combine(findSelected(group1), findSelected(group2)));
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
