package Painter.Screen.UndoRedo;

import java.awt.*;

public class UndoPixelElement extends UndoElement<UndoPixelElement> {
    public final Point pos;
    public final byte pixel,attr;
    public final byte newPixel,newAttr;

    UndoPixelElement(UndoRedo.Client<UndoPixelElement> handler, int x, int y, byte pixel, byte attr,
                            byte newPixel, byte newAttr) {
        super(handler);
        this.pos = new Point(x,y);
        this.pixel = pixel;
        this.attr = attr;
        this.newPixel = newPixel;
        this.newAttr = newAttr;
    }

    @Override
    void undo() {
        handler.undo(this);
    }

    @Override
    void redo() {
        handler.redo(this);
    }
}

