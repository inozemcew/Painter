package NPainter;

import Painter.Palette.Palette;

/**
 * Created by ainozemtsev on 04.07.16.
 */
public class NPalette extends Palette {

    private static final int[] tSize = {8, 8};
    private static final int[] cSize = {2, 2};

    public NPalette() {
        super(2, tSize, cSize);
        int[] ink = new int[8], paper = new int[8];
        for (int i = 0; i<8; i++) {
            paper[i] = (i+16) | ((i+48)<<6);
            ink[i] = (i+40) | ((i+56)<<6);
        }
        setPalette(ink,paper);
    }

}
