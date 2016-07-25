package NPainter;

import NPainter.Convert.ConvertDialog;
import NPainter.Convert.ImageConverter;
import Painter.PainterApp;
import Painter.Screen;

import javax.imageio.ImageIO;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by ainozemtsev on 30.06.16.
 */
public class NPainter extends PainterApp {
    @Override
    protected Screen createScreen() {
        return new NScreen();
    }

    public static void main(String[] argv) {
        run(new NPainter());
    }

    @Override
    protected DataInputStream convertPNGStream(FileInputStream stream) throws IOException {
        ImageConverter converter = new ImageConverter(ImageIO.read(stream));
        ConvertDialog convertDialog = new ConvertDialog(converter);
        return convertDialog.runDialog() ? converter.asTileStream() : null;
    }
}
