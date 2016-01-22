package Painter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * Created by ainozemtsev on 17.11.15.
 */

public  class PainterApp {
    JFrame frame = new JFrame("Painter");
    JLabel statusBar = new JLabel(" ");
    private PaintArea paintArea;
//    private Palette palette;
    private final JFileChooser fileChooser = new JFileChooser();

    public static void main(String[] argv) {
        PainterApp form = new PainterApp();
        JFrame frame = form.createMainForm();
        frame.setVisible(true);
    }

    JFrame createMainForm() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel form = new JPanel(new BorderLayout());

        Screen screen = new Screen();
        this.paintArea = new PaintArea(screen);
        form.add(new JScrollPane(this.paintArea), BorderLayout.CENTER);


        InterlacedView view = new InterlacedView(screen);
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                super.mousePressed(mouseEvent);
                paintArea.ScrollInView(mouseEvent.getX() / 2, mouseEvent.getY() / 2);
            }
        });
        form.add(view, BorderLayout.LINE_START);


        JToolBar toolbar = new JToolBar();
//        this.palette = new Palette();
        toolbar.add(this.paintArea.createToolBar());
        toolbar.addPropertyChangeListener(paintArea.new Listener());


        JSlider spinner = new JSlider(1, 16, 2);
        spinner.setPreferredSize(new Dimension(40, 36));
        spinner.setMajorTickSpacing(1);
        spinner.setPaintTicks(true);
        spinner.setPaintLabels(true);
        spinner.setValue(paintArea.getScale());
        spinner.addChangeListener(e -> this.paintArea.setScale(spinner.getValue()));
        toolbar.add(spinner);
        toolbar.add(new JPanel());

        form.add(toolbar, BorderLayout.NORTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu file = menuBar.add(new JMenu("File"));
        file.setMnemonic('F');
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
        undo.addActionListener(event -> paintArea.undo());
        JMenuItem redo = edit.add("Redo");
        redo.setAccelerator(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
        redo.addActionListener(event -> paintArea.redo());

        frame.setJMenuBar(menuBar);

        form.add(statusBar, BorderLayout.PAGE_END);
        paintArea.addPropertyChangeListener("status", evt -> statusBar.setText(evt.getNewValue().toString()));

        frame.add(form);
        frame.pack();
        return frame;
    }

    private void importSCR() {
        fileChooser.setDialogTitle("Choose spectrum screen to import");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Spectrum screen", "scr");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(frame, "Open")) {
            File file = fileChooser.getSelectedFile();
            try {
                paintArea.importSCR(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.frame,
                        "Cannot import " + file,
                        "Import error",
                        JOptionPane.ERROR_MESSAGE);
            }
            frame.repaint();
        }
    }
    private void importPNG() {
        fileChooser.setDialogTitle("Choose PNG-image to import");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG-image", "png");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(frame, "Open")) {
            File file = fileChooser.getSelectedFile();
            try {
                paintArea.importPNG(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.frame,
                        "Cannot import " + file,
                        "Import error",
                        JOptionPane.ERROR_MESSAGE);
            }
            frame.repaint();
        }
    }


    private void saveAs() {
        fileChooser.setDialogTitle("Save As");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("New screen", "scrn");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(frame, "Save")) {
            File file = fileChooser.getSelectedFile();
            try {
                paintArea.save(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.frame,
                        "Cannot save " + file,
                        "Save error",
                        JOptionPane.ERROR_MESSAGE);
            }
            frame.repaint();
        }
    }

    private void load() {
        fileChooser.setDialogTitle("Load screen");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("New screen", "scrn");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(frame, "Load")) {
            File file = fileChooser.getSelectedFile();
            try {
                paintArea.load(file);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this.frame,
                        "Cannot load " + file,
                        "Load error",
                        JOptionPane.ERROR_MESSAGE);
            }
            frame.repaint();
        }
    }

}