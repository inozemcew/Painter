package APainter;

import Painter.InterlacedView;
import Painter.PainterApp;
import Painter.Screen.Palette.ColorConverter;
import Painter.Screen.Screen;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ainozemtsev on 30.06.16.
 */
public class APainter extends PainterApp {

    private final CharScreen charScreen = new CharScreen();

    @Override
    protected Screen createScreen() {
        return new AScreen();
    }

    @Override
    protected JFrame createMainForm() {
        JFrame frame = super.createMainForm();
        final InterlacedView view = new InterlacedView(charScreen);
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                paintArea.getClipCell().setLocation(charScreen, e.getX() / view.getScale(), e.getY() / view.getScale());
            }
        });
        frame.add(view, BorderLayout.LINE_END);
        return frame;
    }

    @Override
    protected ColorChangeAdapter createColorChangeAdapter() {
        return new ColorChangeAdapter() {
            @Override
            public void colorChanged(int table, int index) {
                super.colorChanged(table, index);
                charScreen.setAttr(paintArea.getCurrentColorIndex(AScreen.Table.Fore));
            }
        };
    }

    public static void main(String[] argv) {
        run(new APainter());
    }

    @Override
    protected DataInputStream convertPNGStream(FileInputStream stream) throws IOException {
        BufferedImage img = ImageIO.read(stream);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bs);
        this.screen.getPalette().savePalette(os);
        os.writeInt(img.getWidth()/3);
        os.writeInt(img.getHeight()/2);

        ColorConverter converter = new ColorConverter();
        for (int x = 0; x < img.getWidth()-2; x+=3) {
            for (int y = 0; y < img.getHeight()-1; y+=2) {
                int[] RGBs = img.getRGB(x,y,3,2,null,0,3);
                int[] pixels = Arrays.stream(RGBs).map(i -> converter.fromRGB(new Color(i),AScreen.colors)).toArray();
                int maxI = 0, maxV = 0;
                Map<Integer,Integer> counts = new HashMap<>();
                for (int i = 0; i < pixels.length; i++) {
                    int k = pixels[i];
                    int v = counts.getOrDefault(k,0) + 1;
                    counts.put(k, v);
                    if (k>0 && v>maxV) {
                        maxI = k;
                        maxV = v;
                    }
                }
                int[] indices = {maxI, 0};
                int b = 0;
                for (int i = 0; i < 6; i++) {
                    b = (b << 1) | (pixels[5 - i] > 0 ? 1 : 0);
                }
                os.write(b);
                os.write(maxI);

            }

        }
        return new DataInputStream(new ByteArrayInputStream(bs.toByteArray()));
    }
}
