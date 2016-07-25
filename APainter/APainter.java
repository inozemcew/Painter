package APainter;

import Painter.PainterApp;
import Painter.Screen;
import Painter.InterlacedView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by ainozemtsev on 30.06.16.
 */
public class APainter extends PainterApp {
    @Override
    protected Screen createScreen() {
        return new AScreen();
    }

    @Override
    protected JFrame createMainForm() {
        JFrame frame = super.createMainForm();
        CharScreen screen = new CharScreen();
        final InterlacedView view = new InterlacedView(screen);
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                screen.setAttr(paintArea.getColorIndex(AScreen.Table.Fore));
                paintArea.getClipCell().setLocation(screen,e.getX()/view.getScale(),e.getY()/view.getScale());
            }
        });
        frame.add(view, BorderLayout.LINE_END);
        return frame;
    }

    public static void main(String[] argv) {
        run(new APainter());
    }

    @Override
    protected DataInputStream convertPNGStream(FileInputStream stream) throws IOException {
        return null;
    }
}
