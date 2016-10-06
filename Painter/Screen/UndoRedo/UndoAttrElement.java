package Painter.Screen.UndoRedo;

import java.awt.*;

/**
 * Created by ainozemtsev on 05.10.16.
 */
public class UndoAttrElement extends UndoElement<UndoAttrElement> {
    public final Point pos;
    public final byte attr, newAttr;

    UndoAttrElement(UndoRedo.Client<UndoAttrElement> handler, int x, int y, byte attr, byte newAttr) {
        super(handler);
        this.pos = new Point(x, y);
        this.attr = attr;
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
