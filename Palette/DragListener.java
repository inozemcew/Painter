package Painter.Palette;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Created by ainozemtsev on 04.02.16.
 */
class DragListener extends MouseInputAdapter {
    JComponent container;
    Component selectedComponent;
    BiConsumer<Integer,Integer> listener;
    int selectedIndex;
    int index;
    Point start;
    enum State { NO, STAGE, DRAG }
    State dragging = State.NO;
    final int MIN_DRAG_DISTANCE = 5;

    public DragListener(JComponent dt, BiConsumer<Integer, Integer> listener) {
        this.container = dt;
        this.listener = listener;
        this.dragging = State.NO;

    }

    /*interface Reordering {
        void reorder(int oldIndex, int newIndex);
    }*/

    public /*<T extends Component & Reordering>*/ void addComponent(Component comp) {
        this.selectedComponent = comp;
        //this.listener = comp;
        //comp.addColorChangeListener(e -> fireColorChange(b.getTable(), b.getIndex()));
        comp.addMouseListener(this);
        comp.addMouseMotionListener(this);

    }

    public void mousePressed(MouseEvent e) {
        Point p = getPoint(e);
        Component[] c = container.getComponents();
        for (int j = 0; j < c.length; j++) {
            Rectangle r = c[j].getBounds();
            if (r.contains(p)) {
                selectedIndex = j;
                selectedComponent = c[j];
                start = e.getPoint();
                dragging = State.STAGE;
                break;
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (dragging == State.STAGE) {
            if (e.getPoint().distance(start) > MIN_DRAG_DISTANCE) {
                dragging = State.DRAG;
            }
        }
        if (dragging == State.DRAG) {
            Point p = getPoint(e);
            if (!container.contains(p)) return;
            Component comp = container.getComponentAt(p);
            if (comp == selectedComponent) return;

            if (comp.getClass() != selectedComponent.getClass())
                index = getDropIndex(container, p);
            else  // over a child component
            {
                index = getComponentIndex(container, comp);
            }
            if (index>=0) {
                container.setComponentZOrder(selectedComponent, index);
                container.revalidate();
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (dragging == State.STAGE ) { // no component has been removed
            dragging = State.NO;
            return;
        }
        listener.accept(selectedIndex,index);
        container.setComponentZOrder(selectedComponent,selectedIndex);
        container.revalidate();
    }

    Point getPoint(MouseEvent e) {
        Point p = e.getPoint();
        final Component c = e.getComponent();
        p.translate(c.getX(), c.getY());
        return p;
    }

    private int getDropIndex(Container parent, Point p) {
        Component[] c = parent.getComponents();
        for (int j = 0; j < c.length; j++) {
            if (c[j].getX() > p.x && c[j].getY() > p.y) return j;
            if (c[j].getY() > p.y) return j-1;
        }
        return index;
    }

    private int getComponentIndex(Container parent, Component target) {
        Component[] c = parent.getComponents();
        for (int j = 0; j < c.length; j++)
            if (c[j] == target)
                return j;
        return index;
    }
}