package Painter.Screen;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

/**
 * Created by aleksey on 05.12.15.
 */
public class UndoRedo {

    public interface Item {
        void undo();
        void redo();
    }

    private final LinkedList<Vector<Item>> undoStack= new LinkedList<>();
    private final LinkedList<Vector<Item>> redoStack= new LinkedList<>();

    private Vector<Item> current = null;

    public void start(){
        current = new Vector<>();
        redoStack.clear();
    }

    public void add(Item client) {
        if (current != null) {
            current.add(client);
        }
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
            Vector<Item> e = undoStack.removeFirst();
            redoStack.addFirst(e);
            for (ListIterator<Item> l = e.listIterator(e.size()); l.hasPrevious(); )
                l.previous().undo();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty() && current == null) {
            Vector<Item> e = redoStack.removeFirst();
            undoStack.addFirst(e);
            for (ListIterator<Item> l = e.listIterator(); l.hasNext(); )
                l.next().redo();
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

