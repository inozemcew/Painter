package Painter.Convert;

import Painter.InterlacedView;
import Painter.Palette.Palette;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

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

        ColorCellModel model = new ColorCellModel(converter.getColorMap());
        ColorCellRenderer renderer = new ColorCellRenderer();

        JTable table = new JTable(model);
        table.setTableHeader(null);

        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);

        table.getColumnModel().getColumn(0).setMinWidth(48);
        table.getColumnModel().getColumn(1).setMinWidth(48);

        JComboBox<Icon> cb = new JComboBox<>();
        for (int i = 0; i < 16; i++) for (int j=0; j<64; j+=16) cb.addItem(model.getToList().get(i+j));

        cb.setMaximumRowCount(24);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(cb));
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(180,80));
        add(scrollPane, BorderLayout.LINE_END);

        JPanel p = new JPanel();
        JButton b = new JButton("OK");
        b.addActionListener(e -> {
            result = true;
            this.setVisible(false);
        });
        p.add(b);
        b = new JButton("Cancel");
        b.addActionListener(e -> {
            result = false;
            this.setVisible(false);
        });
        p.add(b);
        p.add(new JSeparator(SwingConstants.VERTICAL));
        JToggleButton t = new JToggleButton("Preview");
        t.addActionListener(e -> converter.setPreview(t.isSelected()));
        p.add(t);
        add(p, BorderLayout.PAGE_END);
        pack();
    }

    public boolean runDialog() {
        setVisible(true);
        return result;
    }

    static class ColorCellRenderer extends DefaultTableCellRenderer{
        protected void setValue(Object o) {
            setIcon(((ColorIcon) o));
        }

    }

    class ColorCellModel extends AbstractTableModel {
        ArrayList<ColorIcon> from = new ArrayList<>();
        ArrayList<ColorIndexIcon> to = new ArrayList<>();

        public ColorCellModel(Map<Color,Integer> map) {
            super();
            for (Color c : map.keySet()) {
                from.add(new ColorIcon(c));
            }
            for (int i=0; i<64; i++) {
                to.add(new ColorIndexIcon(i));
            }
        }

        ArrayList<ColorIndexIcon> getToList() {
            return this.to;
        }

        @Override
        public int getRowCount() {
            return converter.getColorMap().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return (columnIndex == 0) ? ColorIcon.class : ColorIndexIcon.class;
        }

        @Override
        public Object getValueAt(int r, int c) {
            ColorIcon ci = from.get(r);
            if (c == 0) return ci; else return to.get(converter.getColorMap().get(ci.getColor()));
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return (c>0);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Color color = converter.getColorMap().keySet().toArray(new Color[0])[rowIndex];
            converter.getColorMap().replace(color,((ColorIndexIcon)aValue).getIndex());
        }
    }
}

class ColorIcon implements Icon {
    Color color;

    public ColorIcon(Color color) {
        this.color = color;
    }

    Color getColor() {
        return this.color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(getColor());
        g.fillRect(0, 0, getIconWidth() - 1, getIconHeight() - 1);
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

class ColorIndexIcon extends ColorIcon {
    int index;

    public ColorIndexIcon(int index) {
        super(Palette.toRGB(index));
        this.index = index;
    }
    int getIndex() {
        return this.index;
    }
}