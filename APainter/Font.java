package APainter;

import java.io.*;

/**
 * Created by aleksey on 25.09.16.
 */
class Font {
    private byte[] font = new byte[2048];
    private int offset = 128;
    private final static Font singleton = loadFont(Font.class.getResourceAsStream("/resource/Font.bin"));

    byte getRasterLine(int chr, int line) {
        return font[(((chr + offset) & 255) << 3) + line];
    }

    private static Font loadFont(InputStream is) {
        Font font = new Font();
        try {
            is.read(font.font);
        } catch (IOException e) {
            System.err.printf("Cannot load font file %s", e.getMessage());
        }
        return font;
    }

    static Font loadFont(String fname) {
        return loadFont(new File(fname));
    }

    static Font loadFont(File file) {
        InputStream is;
        try {
            return loadFont(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.printf("Font file %s is not found.", e.getMessage());
            return new Font();
        }

    }

    static Font getFont() {
        return singleton;
    }
}
