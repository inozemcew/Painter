package NPainter.Convert;

import NPainter.NScreen;
import NPainter.PixelProcessor;
import Painter.InterlacedView;
import Painter.Palette.ChangeAdapter;
import Painter.Palette.Palette;
import Painter.Palette.PaletteToolPanel;
import Painter.Screen.Screen;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by aleksey on 24.01.16.
 */
public class ConvertDialog {
    enum Result {OK, CANCEL, BACK, NEXT}

    Result result;
    Screen screen = new NScreen();
    ImageConverter converter;
    JDialog dialog1, dialog2;

    public ConvertDialog(ImageConverter converter) {
        this.converter = converter;
        dialog1 = new ConvertDialog1();
        dialog2 = new ConvertDialog2();
    }

    public boolean runDialog() {
        do {
            result = Result.CANCEL;
            dialog1.setVisible(true);
            if (result == Result.CANCEL) return false;

            if (result == Result.OK) converter.calcPalette(screen.getPalette());
            result = Result.CANCEL;
            dialog2.setVisible(true);
            if (result == Result.CANCEL) return false;
        } while (result ==Result.BACK);
        return true;
    }


    class ConvertDialog1 extends JDialog {
        List<Color> colors = new ArrayList<>();

        public ConvertDialog1() {
            super();
            setTitle("Image conversion - phase 1");
            setModal(true);
            final InterlacedView interlacedView = new InterlacedView(converter);
            add(interlacedView);

            converter.getColorMap().keySet().forEach(colors::add);

            ColorCellModel model = new ColorCellModel(colors);
            model.addTableModelListener(e1 -> {
                if (converter.getPreview()) interlacedView.repaint();
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
                        converter.loadColorMap();
                        model.fireTableDataChanged();
                    });
                    p.add(b, BorderLayout.PAGE_END);
                }
                p.add(scrollPane, BorderLayout.CENTER);
                add(p, BorderLayout.LINE_END);
            }
            { // bottom buttons panel
                JPanel p = new JPanel();

                JButton n = new JButton("Next>");
                n.addActionListener(e -> {
                    result = Result.NEXT;
                    this.setVisible(false);
                });
                n.setVisible(false);
                p.add(n);

                JButton b = new JButton("OK");

                b.addActionListener(e -> {
                    result = Result.OK;
                    n.setVisible(true);
                    this.setVisible(false);
                });
                p.add(b);

                b = new JButton("Cancel");
                b.addActionListener(e -> {
                    result = Result.CANCEL;
                    this.setVisible(false);
                });
                p.add(b);

                p.add(new JSeparator(SwingConstants.VERTICAL));

                JToggleButton t = new JToggleButton("Preview");
                t.addActionListener(e -> converter.setPreview(t.isSelected()));
                p.add(t);

                add(p, BorderLayout.PAGE_END);
            }
            pack();
        }

        class ColorCellModel extends AbstractTableModel {
            ArrayList<ColorIcon> fromIcons = new ArrayList<>();
            ArrayList<ColorIndexIcon> toIcons = new ArrayList<>();

            public ColorCellModel(List<Color> colors) {
                super();
                colors.forEach(c -> this.fromIcons.add(new ColorIcon(c)));
                IntStream.range(0, 64).forEach(i -> toIcons.add(new ColorIndexIcon(i)));
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
                if (c == 0) return ci;
                else {
                    final Integer index = converter.getColorMap().get(ci.getColor());
                    return toIcons.get(index);
                }
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return (c > 0);
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                Color color = fromIcons.get(rowIndex).getColor();
                converter.getColorMap().replace(color, ((ColorIndexIcon) aValue).getIndex());
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }
    }

    class ConvertDialog2 extends JDialog {

        public ConvertDialog2() {
            super();
            setTitle("Image conversion - phase 2");
            setModal(true);
            screen.setPixelProcessing(PixelProcessor.MODE4);
            final InterlacedView interlacedView = new InterlacedView(screen);
            //reloadScreen();
            add(interlacedView);
            { // upper toolbar
                JToolBar toolBar = new JToolBar();
                PaletteToolPanel panel = new PaletteToolPanel(screen.getPalette());

                ChangeAdapter changeAdapter = new ChangeAdapter(screen);
                panel.addChangeListener(changeAdapter);

                JToggleButton b = new JToggleButton(changeAdapter.createAction());
                panel.add(b);

                toolBar.add(panel);
                add(toolBar,BorderLayout.PAGE_START);
            }

            { // bottom buttons panel
                JPanel p = new JPanel();

                JButton b = new JButton("<Back");
                b.addActionListener(e -> {
                    result = Result.BACK;
                    this.setVisible(false);
                });
                p.add(b);
                p.add(new JSeparator(SwingConstants.VERTICAL));

                b = new JButton("OK");

                b.addActionListener(e -> {
                    result = Result.OK;
                    this.setVisible(false);
                });
                p.add(b);

                b = new JButton("Cancel");
                b.addActionListener(e -> {
                    result = Result.CANCEL;
                    this.setVisible(false);
                });
                p.add(b);

                p.add(new JSeparator(SwingConstants.VERTICAL));

                b = new JButton("Update");
                b.addActionListener(e -> reload());
                p.add(b);

/*                JToggleButton t = new JToggleButton("Preview");
                t.addActionListener(e -> converter.setPreview(t.isSelected()));
                p.add(t);*/

                add(p, BorderLayout.PAGE_END);
            }
            pack();

        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) reload();
            super.setVisible(visible);
        }

        private void reload() {
             try {
                screen.importImage(converter.asTileStream());
            } catch (IOException e) { }
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