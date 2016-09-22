package Painter.Screen;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by aleksey on 05.12.15.
 */
public class UndoRedo {
    private final LinkedList<Vector<UndoElement>> undoStack= new LinkedList<>();
    private final LinkedList<Vector<UndoElement>> redoStack= new LinkedList<>();

    private Vector<UndoElement> current = null;

    public void start(){
        current = new Vector<>();
        redoStack.clear();
    }

    public void add(int x, int y, byte pixel, byte attr, byte newPixel, byte newAttr) {
        if (current != null) {
            current.add(new UndoElement(x, y, pixel, attr, newPixel, newAttr));
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

    public Vector<UndoElement> undo() {
        if (!undoStack.isEmpty()) {
            Vector<UndoElement> e = undoStack.removeFirst();
            redoStack.addFirst(e);
            return e;
        } else return null;
    }

    public Vector<UndoElement> redo() {
        if (!redoStack.isEmpty() && current == null) {
            Vector<UndoElement> e = redoStack.removeFirst();
            undoStack.addFirst(e);
            return e;
        }
        return null;
    }
}

class UndoElement {
    int x,y;
    byte pixel,attr;
    byte newPixel,newAttr;

    public UndoElement(int x, int y, byte pixel, byte attr, byte newPixel, byte newAttr) {
        this.x = x;
        this.y = y;
        this.pixel = pixel;
        this.attr = attr;
        this.newPixel = newPixel;
        this.newAttr = newAttr;
    }
}

