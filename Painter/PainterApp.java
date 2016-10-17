package Painter;

import Painter.PaletteControls.ChangeAdapter;
import Painter.PaletteControls.PaletteToolPanel;
import Painter.Screen.PixelProcessing;
import Painter.Screen.Screen;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;

import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.SELECTED_KEY;

/**
 * Created by ainozemtsev on 17.11.15.
 */

public abstract class PainterApp extends JFrame {
    StatusBar statusBar = new StatusBar(2);

    protected Screen screen = createScreen();
    private final JFileChooser fileChooser = new JFileChooser();
    private InterlacedView interlacedView;
    private JSplitPane splitPane;
    private JSlider scaleSlider;

    protected PaintArea paintArea;

    private Actions actions;
    private HashMap<String,PropertyChangeListener> propertyChangeListeners = new HashMap<>();

    private RecentFiles recentFiles = new RecentFiles(this);

    {
        propertyChangeListeners.put(PaintArea.OP_STATUS, evt -> statusBar.setText(evt.getNewValue().toString(), 0));
        propertyChangeListeners.put(PaintArea.OP_SCALE, evt -> scaleSlider.setValue((Integer)evt.getNewValue()));
        propertyChangeListeners.put(PaintArea.OP_FILL, evt ->  actions.editModes.reset());
        propertyChangeListeners.put(PaintArea.OP_SWAP, evt ->  actions.editModes.reset());
    }

    public static void run(PainterApp app) {
        SwingUtilities.invokeLater(new Runnable() {
                                       @Override
                                       public void run() {
                                           JFrame frame = app.createMainForm();
                                           frame.setVisible(true);
                                       }
                                   }
        );
    }

    public PainterApp() throws HeadlessException {
        super();
//        this.screen = createScreen();
    }

    protected abstract Screen createScreen();

    protected JFrame createMainForm() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Painter for new screen");

        JPanel form = new JPanel(new BorderLayout());

        paintArea = new PaintArea(screen);
        propertyChangeListeners.forEach( (name, listener) -> paintArea.addPropertyChangeListener(name, listener));

        actions = new Actions();

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

        add(form);
        pack();
        return this;
    }

    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();

        ButtonGroup modeGroup = new ButtonGroup();
        actions.editModes.forEach(action -> {
            final AbstractButton b = new JToggleButton(action);
            toolbar.add(b);
            modeGroup.add(b);
        });

        final PaletteToolPanel panel = new PaletteToolPanel(screen.getPalette());

        panel.addChangeListener(actions.changeAdapter);

        toolbar.addPropertyChangeListener("orientation", evt -> panel.setOrientation((Integer)evt.getNewValue()));
        toolbar.add(panel);

        scaleSlider = new JSlider(1, 16, 2);
        scaleSlider.setPreferredSize(new Dimension(96, 36));
        scaleSlider.setMajorTickSpacing(1);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setValue(paintArea.getScale());
        scaleSlider.addChangeListener(e -> paintArea.setScale(scaleSlider.getValue()));
        toolbar.add(scaleSlider);

        //panel.addChangeListener(action);
        JToggleButton button = new JToggleButton(actions.viewEnhance);
        toolbar.add(button);

        toolbar.add(Box.createGlue()); //  Separator();
        toolbar.add(paintArea.getClipCell().getClipComponent());
        return toolbar;
    }

    private RecentFilesMenuItems recentFilesMenuItems;
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = menuBar.add(new JMenu("File"));
        file.setMnemonic('F');
        JMenu n = new JMenu("New");
        screen.getResolutions().forEach((s,d)-> n.add(s).addActionListener(e -> newScreen(d.width,d.height)));
        file.add(n);
        file.add(actions.fileLoad);
        file.add(actions.fileSave);
        file.add(actions.fileSaveAs);
        file.add(actions.fileImportSCR);
        file.add(actions.fileImportPNG);
        file.addSeparator();
        recentFilesMenuItems = new RecentFilesMenuItems(file);
        recentFilesMenuItems.getMenuItems().forEach(file::add);
        file.addSeparator();
        file.add(actions.fileExit);

        JMenu edit = menuBar.add(new JMenu("Edit"));
        edit.setMnemonic('E');
        edit.add(actions.editUndo);
        edit.add(actions.editRedo);
        edit.addSeparator();



        ButtonGroup group = new ButtonGroup();
        actions.editModes.forEach(action -> {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(action);
            menuItem.addMenuKeyListener(new MenuKeyListener() {
                @Override
                public void menuKeyTyped(MenuKeyEvent e) {

                }

                @Override
                public void menuKeyPressed(MenuKeyEvent e) {
                    if (e.getKeyChar() == ((KeyStroke)(action.getValue(ACCELERATOR_KEY))).getKeyChar()
                            && e.getModifiers() == KeyEvent.SHIFT_DOWN_MASK)
                        menuItem.doClick();
                }

                @Override
                public void menuKeyReleased(MenuKeyEvent e) {

                }
            });
            group.add(edit.add(menuItem));
        });

        edit.addSeparator();

        actions.specialMethods.forEach(edit::add);
        edit.addSeparator();

        JMenu sh = new JMenu("Shift");
        sh.add("Left").addActionListener(e -> screen.shift(Screen.Shift.Left));
        sh.add("Right").addActionListener(e -> screen.shift(Screen.Shift.Right));
        sh.add("Up").addActionListener(e -> screen.shift(Screen.Shift.Up));
        sh.add("Down").addActionListener(e -> screen.shift(Screen.Shift.Down));
        edit.add(sh);

        if (!actions.screenModes.isEmpty()) {
            JMenu options = menuBar.add(new JMenu("Options"));
            ButtonGroup g = new ButtonGroup();
            actions.screenModes.forEach(action -> g.add(options.add(new JRadioButtonMenuItem(action))));
        }
        return menuBar;

    }

    private class RecentFilesMenuItems implements Observer {
        private JMenu parent;
        private int position;
        private List<JMenuItem> menuItems = new ArrayList<>(5);

        RecentFilesMenuItems(JMenu parent) {
            this.parent = parent;
            this.position = parent.getItemCount();
            updateItems();
            recentFiles.addObserver(this);
        }

        List<JMenuItem> getMenuItems() {
            return menuItems;
        }

        void updateItems() {
            menuItems.clear();
            for (Iterator<String> s = recentFiles.getIterator(); s.hasNext(); ) {
                String i = s.next();
                final JMenuItem item = new JMenuItem(i);
                item.addActionListener(e -> load(i));
                menuItems.add(item);
            }

        }

        void updateMenu() {
            menuItems.forEach(parent::remove);
            updateItems();
            for (int i = 0; i < menuItems.size(); i++) {
                parent.insert(menuItems.get(i), position + i);
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            updateMenu();
        }
    }

    private File currentFile = null;
    private void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        statusBar.setText((currentFile != null) ? currentFile.getName():"",1);
    }


    private void newScreen(int x, int y) {
        screen.newImageBuffer(x, y);
        interlacedView.updatePreferredSize();
        Dimension preferredSize = interlacedView.getPreferredSize();
        preferredSize.setSize(preferredSize.getWidth()+3,preferredSize.getHeight()+3);
        interlacedView.getParent().getParent().setPreferredSize(preferredSize);
        paintArea.updatePreferredSize();
        splitPane.resetToPreferredSizes();
        setCurrentFile(null);
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
            setCurrentFile(null);
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
            setCurrentFile(null);
        }
    }

    protected abstract DataInputStream convertPNGStream(FileInputStream stream) throws IOException;

    private void save() {
        if (currentFile == null)
            saveAs();
        else
            saveAs(currentFile);
    }

    private void saveAs() {
        fileChooser.setDialogTitle("Save As");
        FileNameExtensionFilter filter = screen.getFileNameExtensionFilter();
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, "Save")) {
            File file = fileChooser.getSelectedFile();
            saveAs(file);
        }
    }

    private void saveAs(File file) {
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
        recentFiles.add(file.getAbsolutePath());
        recentFilesMenuItems.updateMenu();
        setCurrentFile(file);
    }

    private void load() {
        fileChooser.setDialogTitle("Load screen");
        FileNameExtensionFilter filter = screen.getFileNameExtensionFilter();
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, "Load")) {
            File file = fileChooser.getSelectedFile();
            load(file);
        }
    }

    private void load(File file) {
        try {
            final FileInputStream fs = new FileInputStream(file);
            InputStream stream = new BufferedInputStream(fs);
            screen.load(stream, fs.getChannel().size() == 50266);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot load " + file,
                    e.getMessage(),
                    JOptionPane.ERROR_MESSAGE);
        }
        repaint();
        recentFiles.add(file.getAbsolutePath());
        recentFilesMenuItems.updateMenu();
        setCurrentFile(file);
    }

    private void load(String fName) {
        load(new File(fName));
    }

    private class ColorChangeAdapter extends ChangeAdapter {
        public ColorChangeAdapter() {
            super(screen);
        }

        @Override
        public void colorChanged(int table, int index) {
            super.colorChanged(table, index);
            paintArea.colorChanged(table, index);
        }
    }

    class Actions {
        final ChangeAdapter changeAdapter = new ColorChangeAdapter();
        Action fileExit = new AbstractAction("Exit") {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('Q', InputEvent.CTRL_DOWN_MASK));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        Action fileLoad = new AbstractAction("Load ..") {
            @Override
            public void actionPerformed(ActionEvent e) {
                load();
            }
        };
        Action fileSave = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        };
        Action fileSaveAs = new AbstractAction("Save as..") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        };
        Action fileImportSCR = new AbstractAction("Import SCR..") {
            @Override
            public void actionPerformed(ActionEvent e) {
                importSCR();
            }
        };
        Action fileImportPNG = new AbstractAction("Import PNG..") {
            @Override
            public void actionPerformed(ActionEvent e) {
                importPNG();
            }
        };

        Action editUndo = new AbstractAction("Undo") {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                screen.undoDraw();
                getContentPane().repaint();
            }
        };
        Action editRedo = new AbstractAction("Redo") {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
                setEnabled(false);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                screen.redoDraw();
                getContentPane().repaint();
            }
        };

        {
            screen.addUndoListener((o, arg) -> {
                editRedo.setEnabled(screen.isRedoEnabled());
                editUndo.setEnabled(screen.isUndoEnabled());
            });
        }

        EditModeActionList editModes = new EditModeActionList(paintArea);
        {
            editModes.addAction("Paint", PaintArea.Mode.Paint, "A");
            editModes.addAction("Swap", PaintArea.Mode.Swap, "S");
            editModes.addAction("Flood", PaintArea.Mode.Fill, "D");
        }


        Action viewEnhance = changeAdapter.createAction();

        ArrayList<Action> specialMethods = new ArrayList<>();
        {
            screen.getSpecialMethods().forEach( (name, consumer) -> specialMethods.add(new AbstractAction(name) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    consumer.accept(paintArea.getColorIndices());
                }
            }) );
        }

        ArrayList<Action> screenModes = new ArrayList<>();
        {
            final PixelProcessing pixelProcessor = screen.getPixelProcessor();
            final Set<? extends PixelProcessing> mode = pixelProcessor.enumPixelModes();
            if (mode != null)
                for (PixelProcessing m: mode ) {
                    Action c = new AbstractAction(m.toString()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            screen.setPixelProcessing(m);
                        }
                    };
                    c.putValue(SELECTED_KEY, (m == pixelProcessor));
                    screenModes.add(c);
            }

        }

    }

}

class EditModeActionList extends ArrayList<EditModeActionList.Action> {
    PaintArea paintArea;
    boolean fixed = false;

    class Action extends AbstractAction {
        PaintArea.Mode mode;

        Action(String name, PaintArea.Mode mode) {
            super(name);
            this.mode = mode;
            putValue(SELECTED_KEY, Boolean.TRUE);
        }

        Action(String name, PaintArea.Mode mode, String acc) {
            this(name, mode);
            putValue(SELECTED_KEY, Boolean.FALSE);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(acc));
            putValue(MNEMONIC_KEY, KeyEvent.getExtendedKeyCodeForChar(acc.charAt(0)));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (paintArea.getMode() == mode) {
                fixed = true;
            } else
                fixed = false;
                paintArea.setMode(mode);
        }

    }

    public EditModeActionList(PaintArea paintArea) {
        this.paintArea = paintArea;
    }

    void addAction(String name, PaintArea.Mode mode) {
        add(new Action(name,mode));
    }

    void addAction(String name, PaintArea.Mode mode, String acc) {
        add(new Action(name,mode,acc));
    }

    public void reset() {
        if (fixed) return;
        final EditModeActionList.Action paintAction = get(0);
        paintAction.putValue(SELECTED_KEY, Boolean.TRUE);
        paintArea.setMode(paintAction.mode);
    }
}

class StatusBar extends Box {

    StatusBar(int panesCount) {
        super(BoxLayout.LINE_AXIS);
        Dimension d = new Dimension();
//        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        for (int i = 0; i < panesCount; i++) {
            //final JComponent c = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));


            final JLabel label = new JLabel();
            //c.add(label);
            label.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            //label.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            d.setSize(128, label.getFontMetrics(label.getFont()).getHeight());
            //c.setMinimumSize(d);
            label.setPreferredSize(d);
            label.setMinimumSize(d);
            label.setText("--- ");
            add(label);
            add(Box.createHorizontalStrut(4));
        }
        //add(Box.createHorizontalGlue());
    }

    void setText(String text, int n) {
        //final JComponent c = (JComponent) getComponent(n);
        final JLabel label = (JLabel) getComponent(n);
        label.setText(text);
    }
}