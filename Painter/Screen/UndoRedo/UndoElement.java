package Painter.Screen.UndoRedo;

/**
 * Created by ainozemtsev on 05.10.16.
 */
abstract class UndoElement {
    private UndoRedo.Client handler;

    UndoElement(UndoRedo.Client handler) {
        this.handler = handler;
    }

    void undo() {
        handler.undo(this);
    }

    void redo() {
        handler.redo(this);
    }
}
