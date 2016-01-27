package Painter.Convert;

import Painter.InterlacedView;
import Painter.Palette.PalettePopup;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

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

        ColorCellModel model = new ColorCellModel();
        ColorCellRenderer renderer = new ColorCellRenderer();

        JTable table = new JTable(model);
        table.setTableHeader(null);

        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(1).setCellRenderer(renderer);
        table.getColumnModel().getColumn(0).setMaxWidth(34);
        table.getColumnModel().getColumn(1).setMaxWidth(34);
        //table.setPreferredSize();
        //table.addMouseListener(PalettePopup.createPalettePopup());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(80,80));
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
        public ColorCellRenderer() {
            super();
        }

        @Override
        protected void setValue(Object o) {
            setIcon(new ColorIcon((Color) o));
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

    }

    class ComboCellModel extends DefaultComboBoxModel<Integer> {
        @Override
        public int getSize() {
            return 64;
        }

        @Override
        public Integer getElementAt(int i) {
            return i;
        }
    }

    class ColorCellModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return converter.getColorMap().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int r, int c) {
            Color color = converter.getColorMap().keySet().toArray(new Color[0])[r];
            if (c == 0) return color; else return converter.converter.remap(color);
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return (c>0);
        }
    }
}