package Painter.Screen;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Vector;

/**
 * Created by aleksey on 05.12.15.
 */
public class UndoRedo extends Observable {

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
            setChanged();
            if (undoStack.size() > 1000) {
                undoStack.removeLast();
            }
        }
        notifyObservers();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            setChanged();
            Vector<Item> e = undoStack.removeFirst();
            redoStack.addFirst(e);
            for (ListIterator<Item> l = e.listIterator(e.size()); l.hasPrevious(); )
                l.previous().undo();
        }
        notifyObservers();
    }

    public void redo() {
        if (!redoStack.isEmpty() && current == null) {
            setChanged();
            Vector<Item> e = redoStack.removeFirst();
            undoStack.addFirst(e);
            for (ListIterator<Item> l = e.listIterator(); l.hasNext(); )
                l.next().redo();
        }
        notifyObservers();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        setChanged();
        notifyObservers();
    }

    public boolean isUndoEmpty() {
        return undoStack.isEmpty();
    }

    public boolean isRedoEmpty() {
        return  redoStack.isEmpty();
    }
}

