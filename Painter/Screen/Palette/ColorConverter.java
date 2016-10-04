package Painter.Screen.Palette;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ainozemtsev on 26.07.16.
 */
public class ColorConverter implements ColorConverting {
    private HashMap<Color,Integer> cache = new HashMap<>();

    @Override
    public int fromRGB(Color color) {
        return cache.computeIfAbsent(color, Palette::fromRGB);
    }

    public int fromRGB(Color color, int[] indices) {
        return cache.computeIfAbsent(color, i -> Palette.fromRGB(i,indices));
    }
    public Color remap (Color color) {
        return Palette.toRGB(fromRGB(color));
    }

    public void replace (Color color, int index) {
        cache.replace(color,index);
    }

    public Map<Color,Integer> getColorMap() {
        return cache;
    }
}
