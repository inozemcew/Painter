package Painter.Convert;

import Painter.InterlacedView;

import javax.swing.*;
import java.awt.*;

/**
 * Created by aleksey on 24.01.16.
 */
public class ConvertDialog extends JDialog {
    boolean result = false;
    ImageConverter converter;

    public ConvertDialog(ImageConverter converter) {
        super((Frame) null, "Image conversion", true);
        this.converter = converter;
        add(new InterlacedView(converter));

        JList<Color> list = new JList<>(converter.getColorMap().keySet().toArray(new Color[0]));
        list.setCellRenderer(new ColorCellRenderer());
        add(list,BorderLayout.LINE_END);

        JPanel p = new JPanel();
        JButton b = new JButton("OK");
        b.addActionListener(e -> { result = true; this.setVisible(false);});
        p.add(b);
        b = new JButton("Cancel");
        b.addActionListener(e -> { result = false; this.setVisible(false);});
        p.add(b);
        add(p,BorderLayout.PAGE_END);
        pack();
    }

    public boolean runDialog() {
        setVisible(true);
        return result;
    }
}

class ColorCellRenderer extends JPanel implements ListCellRenderer<Color> {
    Color value = Color.black;
    boolean isSelected = false;

    class ColorIcon implements Icon {
        Color getColor() {
            return value;
        }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(getColor());
            g.fillRect(0,0,getIconWidth()-1,getIconHeight()-1);
            /*if (isSelected) {
                g.setColor(g.getColor().darker());
                g.drawRect(0,0,getIconWidth()-1,getIconHeight()-1);
            }*/
        }

        @Override
        public int getIconWidth() {
            return 32;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

    }

    class ColorIcon2 extends ColorIcon {
        @Override
        Color getColor() {
            return ColorConverter.remap(super.getColor());
        }
    }

    JLabel colorFrom = new JLabel(new ColorIcon());
    JLabel colorTo = new JLabel(new ColorIcon2());

    public ColorCellRenderer() {
        super();
        add(colorFrom);
        add(new JLabel("->"));
        add(colorTo);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Color> list, Color value, int index, boolean isSelected, boolean cellHasFocus) {
        this.value = value;
        //this.isSelected = isSelected;
        if (isSelected) setBorder(BorderFactory.createRaisedBevelBorder());
        else setBorder(BorderFactory.createEmptyBorder());
        return this;
    }
}