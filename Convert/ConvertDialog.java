package Painter.Convert;

import Painter.InterlacedView;
import Painter.Palette.Palette;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by aleksey on 24.01.16.
 */
public class ConvertDialog extends JDialog {
    boolean result = false;
    ImageConverter converter;
    List<Color> colors = new ArrayList<>();


    public ConvertDialog(ImageConverter converter) {
        super();
        setTitle("Image conversion");
        setModal(true);
        this.converter = converter;
        converter.getColorMap().keySet().forEach(colors::add);


        final CardLayout layout = new CardLayout();
        JPanel pager = new JPanel(layout);
        final JPanel page1 = createPage1();
        pager.add(page1);
        final JPanel page2 = createPage2();
        pager.add(page2);
        add(pager);


        { // bottom buttons panel
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p,BoxLayout.LINE_AXIS));

            JButton prev = new JButton("<Prev");
            JButton next = new JButton("Next>");
            prev.setVisible(false);
            prev.addActionListener(e -> {
                layout.previous(pager);
                if (page1.isVisible()) prev.setVisible(false);
                next.setVisible(true);
            });

            next.addActionListener(e -> {
                layout.next(pager);
                if(page2.isVisible()) next.setVisible(false);
                prev.setVisible(true);
            });

            p.add(prev);
            p.add(Box.createHorizontalGlue());

            JButton b = new JButton("OK");

            b.addActionListener(e -> {
                result = true;
                this.setVisible(false);
            });
            p.add(b);

            p.add(Box.createHorizontalBox());

            b = new JButton("Cancel");
            b.addActionListener(e -> {
                result = false;
                this.setVisible(false);
            });
            p.add(b);

            p.add(new JSeparator(SwingConstants.VERTICAL));

            JToggleButton t = new JToggleButton("Preview");
            t.addActionListener(e -> setPreview(t.isSelected()));
            p.add(t);

            p.add(Box.createHorizontalGlue());

            p.add(next);

            add(p, BorderLayout.PAGE_END);
        }
        pack();
    }

    private void setPreview(boolean value) {
        converter.setPreview(value);
    }

    private JPanel createPage1() {
        JPanel panel = new JPanel(new BorderLayout());

        final InterlacedView interlacedView = new InterlacedView(this.converter);
        panel.add(interlacedView);

        ColorCellModel model = new ColorCellModel(colors);
        model.addTableModelListener(e1 -> {
            if (this.converter.getPreview()) interlacedView.repaint();
        });

        JTable table = new JTable(model);
        final TableColumnModel columnModel = table.getColumnModel();
        table.setTableHeader(null);
        { // Table cell renderer
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                protected void setValue(Object o) {
                    final ColorIcon icon = (ColorIcon) o;
                    setIcon(icon);
                    setToolTipText(icon.getColor().toString());
                }
            };

            for (int i = 0; i < 2; i++) {
                columnModel.getColumn(i).setCellRenderer(renderer);
                columnModel.getColumn(i).setMinWidth(48);
            }
        }
        { // Table Cell Editor
            JComboBox<Icon> cb = new JComboBox<>();
            IntStream.range(0, 16)
                    .flatMap(i -> IntStream.range(0, 4).map(j -> i + j * 16))
                    .forEach(index -> cb.addItem(model.getToList().get(index)));

            cb.setMaximumRowCount(24);
            columnModel.getColumn(1).setCellEditor(new DefaultCellEditor(cb));
            table.setFillsViewportHeight(true);
        }
        { // Scroll pane & reset button
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setPreferredSize(new Dimension(128, 80));
            JPanel p = new JPanel(new BorderLayout());
            {
                JButton b = new JButton("Reset");
                b.addActionListener(e -> {
                    this.converter.loadColorMap();
                    model.fireTableDataChanged();
                });
                p.add(b,BorderLayout.PAGE_END);
            }
            p.add(scrollPane, BorderLayout.CENTER);
            panel.add(p, BorderLayout.LINE_END);
        }
        return panel;
    }

    private JPanel createPage2() {
        JPanel panel = new JPanel();

        return panel;
    }

    public boolean runDialog() {
        setVisible(true);
        return result;
    }

    class ColorCellModel extends AbstractTableModel {
        ArrayList<ColorIcon> fromIcons = new ArrayList<>();
        ArrayList<ColorIndexIcon> toIcons = new ArrayList<>();

        public ColorCellModel(List<Color> colors) {
            super();
            colors.forEach(c -> this.fromIcons.add(new ColorIcon(c)));
            IntStream.range(0,64).forEach(i -> toIcons.add(new ColorIndexIcon(i)));
        }

        ArrayList<ColorIndexIcon> getToList() {
            return this.toIcons;
        }

        @Override
        public int getRowCount() {
            return colors.size();
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
            ColorIcon ci = fromIcons.get(r);
            if (c == 0) return ci; else {
                final Integer index = converter.getColorMap().get(ci.getColor());
                return toIcons.get(index);
            }
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return (c>0);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Color color = fromIcons.get(rowIndex).getColor();
            converter.getColorMap().replace(color,((ColorIndexIcon)aValue).getIndex());
            fireTableRowsUpdated(rowIndex,rowIndex);
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