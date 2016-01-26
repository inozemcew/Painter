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
