package Painter;

import javax.swing.*;
import java.awt.*;

/**
 *
 * Created by ainozemtsev on 21.01.16.
 */
public class InterlacedView extends JComponent {
    private ImageSupplier imageSupplier = null;

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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(140,140,100));
        g.fillRect(getX(), getY(), 2*imageSupplier.getImageWidth(), 2*imageSupplier.getImageHeight());
        for (int x = 0; x < imageSupplier.getImageWidth(); x++)
            for (int y = 0; y < imageSupplier.getImageHeight(); y++) {
                Color c = imageSupplier.getPixelColor(x, y);
                ImageSupplier.Status s = imageSupplier.getStatus(x,y);
                g.setColor(c);
                g.drawLine(x * 2, y * 2, x * 2 + ((s == ImageSupplier.Status.Dimmed) ? 0 : 1), y * 2);
                if (s != ImageSupplier.Status.Enhanced)
                    g.setColor(c.darker());
                if (s != ImageSupplier.Status.Dimmed)
                    g.drawLine(x * 2, y * 2 + 1, x * 2 + 1, y * 2 + 1);

            }
    }

}
