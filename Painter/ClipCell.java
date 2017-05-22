package Painter;

import Painter.Screen.Screen;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ainozemtsev on 07.07.16.
 */
public class ClipCell {
    private Point clip = new Point(-1,-1);
    private Screen screen;
    private JComponent clipComponent = new JLabel(new ClipCellIcon());

    public ClipCell(Screen source) {
        setSource(source);
    }

    public void setSource(Screen source) {
        this.screen = source;
    }

    public JComponent getClipComponent() {
        return clipComponent;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setLocation(int x, int y) {
        clip.setLocation(x, y);
        repaint();
    }

    public void setLocation(Screen s,int x, int y) {
        setSource(s);
        setLocation(x, y);
    }

    public int getX() {
        return clip.x;
    }

    public int getY() {
        return clip.y;
    }

    public Point getPosition() {
        return clip;
    }

    public void repaint() {
        clipComponent.repaint();
    }

    private class ClipCellIcon implements Icon {
        @Override
        public int getIconWidth() {
            return 32;
        }

        @Override
        public int getIconHeight() {
            return 32;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (screen.isInImage(clip.x, clip.y)) {
                Dimension gf = screen.getGridFactor();
                Point p = new Point();
                Point ap = screen.align(clip);
                for (int xx = 0; xx < gf.width; xx++) {
                    for (int yy = 0; yy < gf.height; yy++) {
                        p.setLocation(ap.x + xx, ap.y + yy);
                        g.setColor(screen.getPixelColor(p));
                        g.fillRect(x + xx * 4, y + yy * 4, 4, 4);
                    }
                }
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, 32, 32);
            }
        }
    }

}