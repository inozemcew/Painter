package APainter;

import Painter.Palette.Palette;
import Painter.Screen.Pixel;
import Painter.Screen.PixelProcessing;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by aleksey on 25.09.16.
 */
public class PixelProcessor implements PixelProcessing {
    private int fontHeight = 4;

    private PixelProcessor(){

    }

    static PixelProcessor pixelProcessor = new PixelProcessor();
    Font font = Font.getFont();

    public static PixelProcessor createPixelProcessor() {
        return pixelProcessor;
    }
    @Override
    public Set<? extends PixelProcessing> enumPixelModes() {
        HashSet<PixelProcessor> pixelProcessors = new HashSet<>();
        pixelProcessors.add(pixelProcessor);
        return pixelProcessors;
    }

    @Override
    public byte packPixel(Pixel pixel, byte oldPixelData, Point pos) {
        int b = oldPixelData;
        if (oldPixelData >= 64) b = 0;
        final int m = 1 << (pos.x / 2 % 3 + pos.y / 2 % 2 * 3);
        if (pixel.table == AScreen.Table.Fore) b = b | m;
        else b = b & (255 - m);
        return (byte) b;
    }

    @Override
    public byte packAttr(Pixel pixel, byte oldAttrData, Point pos) {
        return (pixel.table == AScreen.Table.Fore) ? (byte) pixel.index : oldAttrData;
    }

    @Override
    public Pixel unpackPixel(byte pixelData, byte attrData, Point pos) {
        byte c = font.getRasterLine(pixelData, pos.y % fontHeight);
        boolean isPaper = ((c & (32 >> (pos.x % 6))) == 0);
        if (isPaper) return new Pixel(AScreen.Table.Back, 0, 0);
        return new Pixel(AScreen.Table.Fore, attrData, 0);
    }

    @Override
    public Color getPixelColor(Pixel pixel, Point pos, Palette palette) {
        if (pixel.table == AScreen.Table.Back)
            return Color.BLACK ;
        else return palette.getRGBColor(AScreen.Table.Fore, pixel.index, 0);
    }
}
