package Painter;

import javax.swing.*;
import java.awt.*;

/**
 *
 * Created by ainozemtsev on 21.01.16.
 */
public class InterlacedView extends JComponent {
    private int scale;
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
                repaint(x * scale, y * scale, w * scale, h * scale);
            }

            @Override
            public void paletteChanged() {
                repaint();
            }
        });
        //addMouseListener(new Listener());
    }

    public int getScale() {
        return scale;
    }

    void updatePreferredSize() {
        scale = imageSupplier.getImageWidth() >320 ? 1:2;
        setPreferredSize(new Dimension(imageSupplier.getImageWidth() * scale, imageSupplier.getImageHeight() * scale));
    }

    @Override
    public void paint(Graphics g) {
        //super.paintComponent(g);
        g.setColor(new Color(140,140,100));
        //g.fillRect(getX(), getY(), scale *imageSupplier.getImageWidth(), scale *imageSupplier.getImageHeight());
        Rectangle r = g.getClipBounds();
        final int sx = Integer.max(0, r.x / scale);
        final int fx = sx + Integer.min(imageSupplier.getImageWidth(), r.width / scale + 1);
        for (int x = sx; x < fx; x++) {
            final int sy = Integer.max(0, r.y / scale);
            final int fy = sy + Integer.min(imageSupplier.getImageHeight(), r.height / scale +1);
            for (int y = sy; y < fy; y++) {
                Color c = imageSupplier.getPixelColor(x, y);
                ImageSupplier.Status s = imageSupplier.getStatus(x,y);
                g.setColor(c);
                g.drawLine(x * scale, y * scale, x * scale + ((s == ImageSupplier.Status.Dimmed) ? 0 : 1), y * scale);
                if (s != ImageSupplier.Status.Enhanced && scale >1 ) {
                    g.setColor(c.darker());
                }
                if (s != ImageSupplier.Status.Dimmed && scale >1 ) {
                    g.drawLine(x * scale, y * scale + 1, x * scale + 1, y * scale + 1);
                }

            }
        }
    }

}
