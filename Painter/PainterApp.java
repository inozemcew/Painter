package Painter;

import Painter.PaletteControls.ChangeAdapter;
import Painter.PaletteControls.PaletteToolPanel;
import Painter.Screen.Palette.Palette;
import Painter.Screen.PixelProcessing;
import Painter.Screen.Screen;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.List;
import java.util.*;

import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.SELECTED_KEY;

/**
 * Created by ainozemtsev on 17.11.15.
 */

public abstract class PainterApp extends JFrame {
    public static final String RESOURCE_CURSORS = "/resource/cursors/";

    StatusBar statusBar = new StatusBar(160,16,400);

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
        propertyChangeListeners.put(PaintArea.OP_STATUS, evt -> statusBar.setText(evt.getNewValue().toString(), StatusBar.POSITION));
        propertyChangeListeners.put(PaintArea.OP_SCALE, evt -> scaleSlider.setValue((Integer)evt.getNewValue()));
        propertyChangeListeners.put(PaintArea.OP_FILL, evt ->  actions.editModes.reset());
        propertyChangeListeners.put(PaintArea.OP_SWAP, evt ->  actions.editModes.reset());
    }

    protected static void run(PainterApp app) {
        SwingUtilities.invokeLater( () -> app.createMainForm().setVisible(true) );
    }

    public PainterApp() throws HeadlessException {
        super();
//        this.screen = createScreen();
    }

    protected abstract Screen createScreen();

    protected JFrame createMainForm() {
        setTitle("Painter for new screen");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (canContinueIfModified()) {
                    dispose();
                    System.exit(0);
                }
            }
        });

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
        {
            JMenu file = menuBar.add(new JMenu("File"));
            file.setMnemonic('F');
            JMenu n = new JMenu("New");
            actions.newScreens.forEach(n::add);
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
        }
        {
            JMenu edit = menuBar.add(new JMenu("Edit"));
            edit.setMnemonic('E');
            edit.add(actions.editUndo);
            edit.add(actions.editRedo);
            edit.addSeparator();

            ButtonGroup group = new ButtonGroup();
            actions.editModes.forEach(action -> group.add(edit.add(new JRadioButtonMenuItem(action))));

            edit.addSeparator();

            actions.specialMethods.forEach(edit::add);
            edit.addSeparator();

            JMenu sh = new JMenu("Shift");
            sh.add("Left").addActionListener(e -> screen.shift(Screen.Shift.Left));
            sh.add("Right").addActionListener(e -> screen.shift(Screen.Shift.Right));
            sh.add("Up").addActionListener(e -> screen.shift(Screen.Shift.Up));
            sh.add("Down").addActionListener(e -> screen.shift(Screen.Shift.Down));
            edit.add(sh);

            JMenu rs = new JMenu("Change resolution");
            actions.resolutions.forEach(rs::add);
            edit.add(rs);
        }
        {
            //if (!actions.screenModes.isEmpty()) {
            JMenu options = menuBar.add(new JMenu("Options"));
            ButtonGroup g = new ButtonGroup();
            actions.screenModes.forEach(action -> g.add(options.add(new JRadioButtonMenuItem(action))));
            //}
            JMenu palettes = new JMenu("Palettes");
            ButtonGroup group = new ButtonGroup();
            actions.palettes.forEach(action -> group.add(palettes.add(new JRadioButtonMenuItem(action))));
            options.addSeparator();
            options.add(palettes);
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
        statusBar.setText((currentFile != null) ? currentFile.getName():"",StatusBar.FILENAME);
    }

    private boolean modified = false;
    private void setModified(boolean modified) {
        this.modified = modified;
        statusBar.setText(modified? "*":" ", StatusBar.MODIFIED);
    }

    private void newScreen(int x, int y) {
        if (!canContinueIfModified()) return;
        screen.newImageBuffer(x, y);
        updatePreferredSize();
        setCurrentFile(null);
    }

    private void changeResolution(int w, int h) {
        if (!canContinueIfModified()) return;
        try {
            screen.changeResolution(w, h);
            updatePreferredSize();
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot resize"+e.getMessage(),
                    "Resize error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePreferredSize() {
        interlacedView.updatePreferredSize();
        Dimension preferredSize = interlacedView.getPreferredSize();
        preferredSize.setSize(preferredSize.getWidth()+3,preferredSize.getHeight()+3);
        interlacedView.getParent().getParent().setPreferredSize(preferredSize);
        paintArea.updatePreferredSize();
        splitPane.resetToPreferredSizes();
    }

    private void importSCR() {
        if (!canContinueIfModified()) return;
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
            setModified(true);
        }
    }
    private void importPNG() {
        if (!canContinueIfModified()) return;
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
            setModified(true);
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
            try (OutputStream stream = new FileOutputStream(file)) {
                screen.save(stream);
            }
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
        setModified(false);
    }

    private void load() {
        if (!canContinueIfModified()) return;
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
        if (!canContinueIfModified()) return;
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
        updatePreferredSize();
        setCurrentFile(file);
        setModified(false);
    }

    private void load(String fName) {
        load(new File(fName));
    }

    boolean canContinueIfModified() {
        if (!modified) return true;
        String[] msg = { "Current screen is modified.", "Do you want to save it?"};
        int result = JOptionPane.showConfirmDialog(this, msg, "Unsaved changes present.", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.CANCEL_OPTION) return false;
        if (result == JOptionPane.YES_OPTION) {
            save();
            return true;
        }
        modified = false;
        return true;
    }

    protected class ColorChangeAdapter extends ChangeAdapter {
        public ColorChangeAdapter() {
            super(screen);
        }

        @Override
        public void colorChanged(int table, int index) {
            super.colorChanged(table, index);
            paintArea.colorChanged(table, index);
        }
    }

    protected ColorChangeAdapter createColorChangeAdapter() {
        return new ColorChangeAdapter();
    }

    class Actions {
        final ChangeAdapter changeAdapter = createColorChangeAdapter();
        Action fileExit = new AbstractAction("Exit") {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('Q', InputEvent.CTRL_DOWN_MASK));
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(PainterApp.this, WindowEvent.WINDOW_CLOSING));
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
                final boolean undoEnabled = screen.isUndoEnabled();
                editUndo.setEnabled(undoEnabled);
                setModified(undoEnabled);
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

        ArrayList<Action> newScreens = new ArrayList<>();
        {
            screen.getResolutions().forEach((s,d)-> newScreens.add(new AbstractAction(s) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    newScreen(d.width,d.height);
                }
            }));
        }

        ArrayList<Action> resolutions = new ArrayList<>();
        {
            screen.getResolutions().forEach( (s, d) -> resolutions.add(new AbstractAction(s) {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    changeResolution(d.width, d.height);
                }
            }));
        }

        ArrayList<Action> screenModes = new ArrayList<>();
        {
            final PixelProcessing pixelProcessor = screen.getPixelProcessor();
            final Set<? extends PixelProcessing> modes = pixelProcessor.enumPixelModes();
            if (modes != null) {
                char k = '1';
                for (PixelProcessing m: modes ) {
                    Action c = new AbstractAction(m.toString()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            screen.setPixelProcessing(m);
                        }
                    };
                    c.putValue(SELECTED_KEY, (m == pixelProcessor));
                    c.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(k++, InputEvent.ALT_DOWN_MASK));
                    screenModes.add(c);
                }
            }

        }

        ArrayList<Action> palettes = new ArrayList<>();
        {
            for (Palette.ColorSpace cs: Palette.ColorSpace.values()) {
                palettes.add(new AbstractAction(cs.getName()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        screen.getPalette().activateColorSpace(cs);
                        repaint();
                    }
                });
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

class StatusBar extends JPanel {
    static final int POSITION = 0, MODIFIED = 1, FILENAME = 2;


    StatusBar(int... panesWidths) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (int panesWidth : panesWidths) {
            final JLabel label = new JLabel();
            label.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            label.setPreferredSize(new Dimension(panesWidth, label.getFontMetrics(label.getFont()).getHeight()));
            add(label);
        }
    }

    void setText(String text, int n) {
        //final JComponent c = (JComponent) getComponent(n);
        final JLabel label = (JLabel) getComponent(n);
        label.setText(text);
    }
}