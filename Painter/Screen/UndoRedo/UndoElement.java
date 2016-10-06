package Painter.Screen.UndoRedo;

/**
 * Created by ainozemtsev on 05.10.16.
 */
abstract class UndoElement<T extends UndoElement<T>> {
    protected UndoRedo.Client<T> handler;

    UndoElement(UndoRedo.Client<T> handler) {
        this.handler = handler;
    }

    abstract void undo();

    abstract void redo();
}
