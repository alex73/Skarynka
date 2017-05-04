package org.alex73.skarynka.scan.ui.book;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SelectionController {
    private final Container pageContainer;
    private Set<Integer> selected = new TreeSet<>();

    private int start = -1, end = -1;

    public SelectionController(Container pageContainer) {
        this.pageContainer = pageContainer;
    }

    void reset() {
        selected.clear();
        start = -1;
        end = -1;
    }

    void clear() {
        for (int i : selected) {
            repaint(i);
        }
        reset();
    }

    void setStart(int index) {
        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
            if (i >= 0) {
                selected.remove(i);
                repaint(i);
            }
        }
        this.start = index;
    }

    void setEnd(int index) {
        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
            if (i >= 0) {
                selected.remove(i);
                repaint(i);
            }
        }
        this.end = index;
        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
            if (i >= 0) {
                selected.add(i);
                repaint(i);
            }
        }
    }

    void change(int index) {
        if (!selected.remove(index)) {
            selected.add(index);
        }
        repaint(index);
    }

    boolean isSelected(int index) {
        return selected.contains(index);
    }

    void addSelectionInterval(int start, int end) {
        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
            selected.add(i);
            repaint(i);
        }
    }

    void repaint(int index) {
        pageContainer.getComponent(index).repaint();
    }

    List<Integer> getSelected() {
        List<Integer> r = new ArrayList<>(selected);
        Collections.sort(r);
        return r;
    }
}
