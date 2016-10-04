package Painter.Screen;
import java.awt.*;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by aleksey on 05.12.15.
 */
public class UndoRedo {

    public interface Entry {   }

    private final LinkedList<Vector<Entry>> undoStack= new LinkedList<>();
    private final LinkedList<Vector<Entry>> redoStack= new LinkedList<>();

    private Vector<Entry> current = null;

    public void start(){
        current = new Vector<>();
        redoStack.clear();
    }

    public void addPixel(int x, int y, byte pixel, byte attr, byte newPixel, byte newAttr) {
        if (current != null) {
            current.add(new UndoPixelElement(x, y, pixel, attr, newPixel, newAttr));
        }
    }

    public void addAttr(int x, int y, byte attr, byte newAttr) {
        if (current != null) {
            current.add(new UndoAttrElement(x, y, attr, newAttr));
        }
    }

    public void addColor(int table, int index, int newValue, int oldValue) {
        if (current != null)
            current.add(new UndoColorElement(table, index, newValue, oldValue));
    }

    public void commit() {
        if (current != null) {
            undoStack.addFirst(current);
            current = null;
            if (undoStack.size() > 1000)
                undoStack.removeLast();
        }
    }

    public Vector<Entry> undo() {
        if (!undoStack.isEmpty()) {
            Vector<Entry> e = undoStack.removeFirst();
            redoStack.addFirst(e);
            return e;
        } else return null;
    }

    public Vector<Entry> redo() {
        if (!redoStack.isEmpty() && current == null) {
            Vector<Entry> e = redoStack.removeFirst();
            undoStack.addFirst(e);
            return e;
        }
        return null;
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

class UndoPixelElement implements UndoRedo.Entry {
    final Point pos;
    final byte pixel,attr;
    final byte newPixel,newAttr;

    public UndoPixelElement(int x, int y, byte pixel, byte attr, byte newPixel, byte newAttr) {
        this.pos = new Point(x,y);
        this.pixel = pixel;
        this.attr = attr;
        this.newPixel = newPixel;
        this.newAttr = newAttr;
    }
}

class UndoAttrElement implements UndoRedo.Entry {
    final Point pos;
    final byte attr, newAttr;

    public UndoAttrElement(int x, int y, byte attr, byte newAttr) {
        this.pos = new Point(x, y);
        this.attr = attr;
        this.newAttr = newAttr;
    }
}

class UndoColorElement implements UndoRedo.Entry {
    final int table;
    final int index;
    final int newValue;
    final int oldValue;

    public UndoColorElement(int table, int index, int newValue, int oldValue) {
        this.table = table;
        this.index = index;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

}