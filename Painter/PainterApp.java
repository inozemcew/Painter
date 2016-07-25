package Painter;

import Painter.Palette.ChangeAdapter;
import Painter.Palette.PaletteToolPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Created by ainozemtsev on 17.11.15.
 */

public abstract class PainterApp extends JFrame {
    JLabel statusBar = new JLabel(" ");

    Screen screen;
    private final JFileChooser fileChooser = new JFileChooser();
    private InterlacedView interlacedView;
    private JSplitPane splitPane;
    protected PaintArea paintArea;

    public static void run(PainterApp app) {
        SwingUtilities.invokeLater(new Runnable() {
                                       @Override
                                       public void run() {
                                           PainterApp form = app;
                                           JFrame frame = form.createMainForm();
                                           frame.setVisible(true);
                                       }
                                   }
        );
    }

    public PainterApp() throws HeadlessException {
        super();
        this.screen = createScreen();
    }

    protected abstract Screen createScreen();

    protected JFrame createMainForm() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Painter for new screen");

        JPanel form = new JPanel(new BorderLayout());

        paintArea = new PaintArea(screen);

        interlacedView = new InterlacedView(screen);
        interlacedView.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                super.mousePressed(mouseEvent);
                final int scale = interlacedView.getScale();
                paintArea.ScrollInView(mouseEvent.getX() / scale, mouseEvent.getY() / scale);
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
            public void colorChanged(int table, int index) {
                super.colorChanged(table, index);
                paintArea.colorChanged(table, index);
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
        rootPane.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("scale")) spinner.setValue((Integer)evt.getNewValue());
                });
        toolbar.add(spinner);

        //panel.addChangeListener(action);
        JToggleButton button = new JToggleButton(action);
        toolbar.add(button);

        toolbar.add(Box.createGlue()); //  Separator();
        toolbar.add(paintArea.getClipCell().getClipComponent());
        return toolbar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = menuBar.add(new JMenu("File"));
        file.setMnemonic('F');
        JMenu n = new JMenu("New");
        screen.getResolutions().forEach((s,d)-> n.add(s).addActionListener(e -> newScreen(d.width,d.height)));
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

        screen.getSpecialMethods().forEach( (name, consumer) -> {
            edit.add(name).addActionListener(e ->
                    consumer.accept(
                            paintArea.getColorIndices()
                    )
            );

        });
        edit.addSeparator();

        JMenu sh = new JMenu("Shift");
        sh.add("Left").addActionListener(e -> screen.shift(Screen.Shift.Left));
        sh.add("Right").addActionListener(e -> screen.shift(Screen.Shift.Right));
        sh.add("Up").addActionListener(e -> screen.shift(Screen.Shift.Up));
        sh.add("Down").addActionListener(e -> screen.shift(Screen.Shift.Down));
        edit.add(sh);

        final Enum mode = screen.getMode();
        if (mode != null) {
            JMenu options = menuBar.add(new JMenu("Options"));
            ButtonGroup g = new ButtonGroup();
            Iterator<Enum> i = EnumSet.allOf(mode.getDeclaringClass()).iterator();
            while (i.hasNext()) {
                Enum m = i.next();
                JRadioButtonMenuItem c = new JRadioButtonMenuItem(m.toString());
                if (m == mode) c.setSelected(true);
                c.addActionListener(e -> screen.setMode(m));
                g.add(c);
                options.add(c);
            }
        }
        return menuBar;
    }

    private void newScreen(int x, int y) {
        screen.newImageBuffer(x, y);
        interlacedView.updatePreferredSize();
        Dimension preferredSize = interlacedView.getPreferredSize();
        preferredSize.setSize(preferredSize.getWidth()+3,preferredSize.getHeight()+3);
        interlacedView.getParent().getParent().setPreferredSize(preferredSize);
        paintArea.updatePreferredSize();
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
                DataInputStream is = convertPNGStream(stream);
                if (is == null) return;
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

    protected abstract DataInputStream convertPNGStream(FileInputStream stream) throws IOException;


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
        FileNameExtensionFilter filter = screen.getFileNameExtensionFilter();
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