package Painter;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ainozemtsev on 21.01.16.
 */
public class InterlacedView extends JComponent {
    ImageSupplier imageSupplier = null;

    public InterlacedView(ImageSupplier supplier) {
        super();
        this.imageSupplier = supplier;
        updatePreferredSize();
        supplier.addChangeListener(new ImageSupplier.ImageChangeListener() {
            @Override
            public void imageChanged() {
                repaint();
            }

            @Override
            public void imageChanged(int x, int y, int w, int h) {
                repaint(x * 2, y * 2, w * 2, h * 2);
            }

            @Override
            public void paletteChanged() {
                repaint();
            }
        });
        //addMouseListener(new Listener());
    }

    void updatePreferredSize() {
        setPreferredSize(new Dimension(imageSupplier.getImageWidth() * 2, imageSupplier.getImageHeight() * 2));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        for (int x = 0; x < imageSupplier.getImageWidth(); x++)
            for (int y = 0; y < imageSupplier.getImageHeight(); y++) {
                Color c = imageSupplier.getPixelColor(x, y);
                g.setColor(c);
                g.drawLine(x * 2, y * 2, x * 2 + 1, y * 2);
                g.setColor(c.darker());
                g.drawLine(x * 2, y * 2 + 1, x * 2 + 1, y * 2 + 1);

            }
    }

}
