package Painter.Screen.UndoRedo;

/**
 * Created by ainozemtsev on 05.10.16.
 */
public class UndoColorElement extends UndoElement<UndoColorElement> {
    public final int table;
    public final int index;
    public final int newValue;
    public final int oldValue;

    UndoColorElement(UndoRedo.Client<UndoColorElement> handler, int table, int index, int newValue, int oldValue) {
        super(handler);
        this.table = table;
        this.index = index;
        this.newValue = newValue;
        this.oldValue = oldValue;
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
