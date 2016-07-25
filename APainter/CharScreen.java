package APainter;


/**
 * Created by ainozemtsev on 08.07.16.
 */
public class CharScreen extends AScreen {
    private static final int SIZE_X = 48;
    private static final int SIZE_Y = 256;
    private byte attr;

    public CharScreen() {
        super(SIZE_X, SIZE_Y);
        final byte attr = 7;
        final byte bAttr = 1;
        final byte bChr = 63;
        byte i = 0;
        for (int y = 0; y < SIZE_Y; y += GRID_FACTOR_Y*2) {
            for (int x = 0; x < SIZE_X; x += GRID_FACTOR_X*2) {
                putPixelData(x, y, i++, attr);
                putPixelData(x+GRID_FACTOR_X, y, bChr, bAttr);
                putPixelData(x, y+GRID_FACTOR_Y, bChr, bAttr);
                putPixelData(x+GRID_FACTOR_X, y+GRID_FACTOR_Y, bChr, bAttr);
            }
        }
    }

    @Override
    protected byte getAttr(int x, int y) {
        return this.attr;
    }

    public void setAttr(int index) {
        final byte oldAttr = 0;
        this.attr = attrFromDesc(new Pixel(Table.Fore, index, 0), oldAttr);
    }
}
