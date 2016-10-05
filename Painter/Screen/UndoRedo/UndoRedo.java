package Painter.Screen.UndoRedo;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

/**
 * Created by aleksey on 05.12.15.
 */
public class UndoRedo {

    public interface Client<T> {
        void undo(T element);
        void redo(T element);
    }

    private final LinkedList<Vector<UndoElement>> undoStack= new LinkedList<>();
    private final LinkedList<Vector<UndoElement>> redoStack= new LinkedList<>();

    private Vector<UndoElement> current = null;

    public void start(){
        current = new Vector<>();
        redoStack.clear();
    }

    public void addPixel(Painter.Screen.UndoRedo.UndoRedo.Client<UndoPixelElement> handler, int x, int y, byte pixel, byte attr, byte newPixel, byte newAttr) {
        if (current != null) {
            current.add(new UndoPixelElement(handler, x, y, pixel, attr, newPixel, newAttr));
        }
    }

    public void addAttr(Painter.Screen.UndoRedo.UndoRedo.Client<UndoAttrElement> handler, int x, int y, byte attr, byte newAttr) {
        if (current != null) {
            current.add(new UndoAttrElement(handler, x, y, attr, newAttr));
        }
    }

    public void addColor(Painter.Screen.UndoRedo.UndoRedo.Client<UndoColorElement> handler, int table, int index, int newValue, int oldValue) {
        if (current != null)
            current.add(new UndoColorElement(handler, table, index, newValue, oldValue));
    }

    public void commit() {
        if (current != null) {
            undoStack.addFirst(current);
            current = null;
            if (undoStack.size() > 1000)
                undoStack.removeLast();
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Vector<UndoElement> e = undoStack.removeFirst();
            redoStack.addFirst(e);
            for (ListIterator<UndoElement> l = e.listIterator(e.size()); l.hasPrevious(); l.previous().undo());
        }
    }

    public void redo() {
        if (!redoStack.isEmpty() && current == null) {
            Vector<UndoElement> e = redoStack.removeFirst();
            undoStack.addFirst(e);
            e.forEach(i -> i.redo());
        }
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public boolean isUndoEmpty() {
        return undoStack.isEmpty();
    }

    public boolean isRedoEmpty() {
        return  redoStack.isEmpty();
    }
}

