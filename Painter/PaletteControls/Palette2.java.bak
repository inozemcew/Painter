package Painter.Palette;

import java.awt.*;

/**
 * Created by ainozemtsev on 05.01.16.
 */
public class Palette2 extends Palette {
    public static Color toRGB(int index) {
        float x=1,y=0,z=0,r,g,b;
        if ((index & 1) != 0) y = 0.5f;
        if ((index & 2) != 0) { x=1-x; y=1-y; z=1-z; }
        switch (index & 12) {
            case 0: { r=y; g=y; b=x; break;}
            case 4: { r=y; g=z; b=x; break;}
            case 8: { r=x; g=y; b=z; break;}
            default:{ r=z; g=x; b=y; break;}
        }
        float l = (index & 48)/96.0f+0.5f;
        return new Color(r*l, g*l, b*l);

    }
}
