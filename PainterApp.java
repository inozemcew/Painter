package Painter;

import Painter.Convert.ConvertDialog;
import Painter.Convert.ImageConverter;
import Painter.Palette.ChangeAdapter;
import Painter.Palette.Palette;
import Painter.Palette.PaletteToolPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

/**
 * Created by ainozemtsev on 17.11.15.
 */

public  class PainterApp extends JFrame {
    JLabel statusBar = new JLabel(" ");

    Screen screen = new Screen();
    private final JFileChooser fileChooser = new JFileChooser();
    private InterlacedView interlacedView;
    private JSplitPane splitPane;
    private PaintArea paintArea;

    public static void main(String[] argv) {
        PainterApp form = new PainterApp();
        JFrame frame = form.createMainForm();
        frame.setVisible(true);
    }

    JFrame createMainForm() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Painter for new screen");

        JPanel form = new JPanel(new BorderLayout());

        paintArea = new PaintArea(screen);

        interlacedView = new InterlacedView(screen);
        interlacedView.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                super.mousePressed(mouseEvent);
                paintArea.ScrollInView(mouseEvent.getX() / 2, mouseEvent.getY() / 2);
            }
        });
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);

        splitPane.setResizeWeight(0);
        JScrollPane pane = new JScrollPane(interlacedView);
        splitPane.setLeftComponent(pane);
        pane = new JScrollPane(paintArea);
        pane.setMinimumSize(new Dimension(128,128));
        pane.setPreferredSize(new Dimension(512,384));
        splitPane.setRightComponent(pane);
        form.add(splitPane);

        final JToolBar toolbar = createToolBar();
        form.add(toolbar, BorderLayout.PAGE_START);

        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        form.add(statusBar, BorderLayout.PAGE_END);
        paintArea.addPropertyChangeListener("status", evt -> statusBar.setText(evt.getNewValue().toString()));

        add(form);
        pack();
        return this;
    }

    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        final PaletteToolPanel panel = new PaletteToolPanel(screen.getPalette());

        ChangeAdapter action = new ChangeAdapter(screen){
            @Override
            public void colorChanged(Palette.Table table, int index) {
                super.colorChanged(table, index);
                paintArea.colorChanged(table,index);
            }
        };

        panel.addChangeListener(action);

        toolbar.addPropertyChangeListener("orientation", evt -> panel.setOrientation((Integer)evt.getNewValue()));
        toolbar.add(panel);

        JSlider spinner = new JSlider(1, 16, 2);
        spinner.setPreferredSize(new Dimension(96, 36));
        spinner.setMajorTickSpacing(1);
        spinner.setPaintTicks(true);
        spinner.setPaintLabels(true);
        spinner.setValue(paintArea.getScale());
        spinner.addChangeListener(e -> paintArea.setScale(spinner.getValue()));
        toolbar.add(spinner);

        //panel.addChangeListener(action);
        JToggleButton button = new JToggleButton(action);
        toolbar.add(button);

        toolbar.add(Box.createGlue()); //  Separator();
        return toolbar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = menuBar.add(new JMenu("File"));
        file.setMnemonic('F');
        JMenu n = new JMenu("New");
        n.add("256x192").addActionListener(event -> newScreen(256,192));
        n.add("320x200").addActionListener(event -> newScreen(320,200));
        n.add("320x240").addActionListener(event -> newScreen(320,240));
        file.add(n);
        file.add("Load ..").addActionListener(event -> this.load());
        file.add("Save as ..").addActionListener(event -> this.saveAs());
        file.add("Import SCR..").addActionListener(event -> this.importSCR());
        file.add("Import PNG..").addActionListener(event -> this.importPNG());
        file.addSeparator();
        file.add("Exit").addActionListener(event -> System.exit(0));

        JMenu edit = menuBar.add(new JMenu("Edit"));
        edit.setMnemonic('E');
        JMenuItem undo = edit.add("Undo");
        undo.setAccelerator(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(event -> {
            screen.undoDraw();
            getContentPane().repaint();
        });
        JMenuItem redo = edit.add("Redo");
        redo.setAccelerator(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
        redo.addActionListener(event -> {
            screen.redoDraw();
            getContentPane().repaint();
        });
        edit.addSeparator();
        /*final int[] revs = {7, 6, 5, 4, 3, 2, 1, 0};
        edit.add("Reverse ink").addActionListener(e1 -> screen.rearrangeColorTable(Palette.Table.INK, revs));
        edit.add("Reverse paper").addActionListener(e1 -> screen.rearrangeColorTable(Palette.Table.PAPER, revs));
*/
        edit.add("Flip ink").addActionListener(e ->
                screen.flipColorCell(Palette.Table.INK, paintArea.getColorIndex(Palette.Table.INK)));
        edit.add("Flip paper").addActionListener(e ->
                screen.flipColorCell(Palette.Table.PAPER, paintArea.getColorIndex(Palette.Table.PAPER)));
        edit.add("Inverse palette").addActionListener(e -> screen.inverseColors());
        edit.add("Swap ink0 <-> paper0").addActionListener(e ->
                screen.swapInkPaper(paintArea.getColorIndex(Palette.Table.INK),
                        paintArea.getColorIndex(Palette.Table.PAPER), 0)
        );
        edit.add("Swap ink1 <-> paper1").addActionListener(e ->
                screen.swapInkPaper(paintArea.getColorIndex(Palette.Table.INK),
                        paintArea.getColorIndex(Palette.Table.PAPER), 1)
        );
        JMenu options = menuBar.add(new JMenu("Options"));
        ButtonGroup g = new ButtonGroup();
        JRadioButtonMenuItem c4 = new JRadioButtonMenuItem("4 colors mode");
        JRadioButtonMenuItem c6 = new JRadioButtonMenuItem("6 colors mode");
        c6.setSelected(true);
        c4.addActionListener(e -> screen.setMode(Screen.Mode.Color4));
        c6.addActionListener(e -> screen.setMode(Screen.Mode.Color6));
        g.add(c4);
        g.add(c6);
        options.add(c4);
        options.add(c6);

        return menuBar;
    }

    private void newScreen(int x, int y) {
        screen.newImageBuffer(x, y);
        interlacedView.updatePreferredSize();
        Dimension preferredSize = interlacedView.getPreferredSize();
        preferredSize.setSize(preferredSize.getWidth()+3,preferredSize.getHeight()+3);
        interlacedView.getParent().getParent().setPreferredSize(preferredSize);
        splitPane.resetToPreferredSizes();
    }

    private void importSCR() {
        fileChooser.setDialogTitle("Choose spectrum screen to import");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Spectrum screen", "scr");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, "Open")) {
            File file = fileChooser.getSelectedFile();
            try {
                final FileInputStream stream = new FileInputStream(file);
                screen.importSCR(stream);
                stream.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot import " + file,
                        "Import error",
                        JOptionPane.ERROR_MESSAGE);
            }
            repaint();
        }
    }
    private void importPNG() {
        fileChooser.setDialogTitle("Choose PNG-image to import");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG-image", "png");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, "Open")) {
            File file = fileChooser.getSelectedFile();
            try {
                final FileInputStream stream = new FileInputStream(file);
                ImageConverter converter = new ImageConverter(ImageIO.read(stream));
                ConvertDialog convertDialog = new ConvertDialog(converter);
                if (!convertDialog.runDialog()) return;
                DataInputStream is = converter.asTileStream();
                screen.importImage(is);
                stream.close();
                repaint();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot import " + file,
                        "Import error",
                        JOptionPane.ERROR_MESSAGE);
            }
            repaint();
        }
    }


    private void saveAs() {
        fileChooser.setDialogTitle("Save As");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("New screen", "scrn");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, "Save")) {
            File file = fileChooser.getSelectedFile();
            try {
                ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
                screen.save(stream);
                stream.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot save " + file,
                        "Save error",
                        JOptionPane.ERROR_MESSAGE);
            }
            repaint();
        }
    }

    private void load() {
        fileChooser.setDialogTitle("Load screen");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("New screen", "scrn");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, "Load")) {
            File file = fileChooser.getSelectedFile();
            try {
                final FileInputStream fs = new FileInputStream(file);
                ObjectInputStream stream = new ObjectInputStream(fs);
                screen.load(stream, fs.getChannel().size() == 50266);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot load " + file,
                        "Load error",
                        JOptionPane.ERROR_MESSAGE);
            }
            repaint();
        }
    }

}