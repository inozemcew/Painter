package Painter;

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
    }

    public void setLocation(Screen s,int x, int y) {
        setSource(s);
        clip.setLocation(x, y);
        repaint();
    }

    public int getX() {
        return clip.x;
    }

    public int getY() {
        return clip.y;
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
                for (int xx = 0; xx < screen.GRID_FACTOR_X; xx++) {
                    for (int yy = 0; yy < screen.GRID_FACTOR_Y; yy++) {
                        g.setColor(screen.getPixelColor(screen.alignX(clip.x) + xx, screen.alignY(clip.y) + yy));
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