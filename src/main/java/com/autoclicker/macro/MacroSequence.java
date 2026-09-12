package com.autoclicker.macro;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lista ordenada de ClickAction que representa uma macro.
 * Publica EVT_CHANGED sempre que a lista é modificada,
 * permitindo que a UI atualize a JTable automaticamente.
 */
public final class MacroSequence {

    public static final String EVT_CHANGED = "macroChanged";

    private final List<ClickAction> actions = new ArrayList<>();
    private final List<PropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

    public void add(ClickAction action) {
        actions.add(action);
        fireChanged();
    }

    public void insert(int index, ClickAction action) {
        actions.add(index, action);
        fireChanged();
    }

    public void remove(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index);
            fireChanged();
        }
    }

    public void moveUp(int index) {
        if (index > 0 && index < actions.size()) {
            Collections.swap(actions, index, index - 1);
            fireChanged();
        }
    }

    public void moveDown(int index) {
        if (index >= 0 && index < actions.size() - 1) {
            Collections.swap(actions, index, index + 1);
            fireChanged();
        }
    }

    public void clear() {
        actions.clear();
        fireChanged();
    }

    public ClickAction get(int index)    { return actions.get(index); }
    public int size()                    { return actions.size(); }
    public boolean isEmpty()             { return actions.isEmpty(); }

    /** Retorna snapshot imutável para iterar com segurança durante execução. */
    public List<ClickAction> snapshot()  { return List.copyOf(actions); }

    public void addPropertyChangeListener(PropertyChangeListener listener)    { listeners.add(listener); }
    public void removePropertyChangeListener(PropertyChangeListener listener) { listeners.remove(listener); }

    private void fireChanged() {
        PropertyChangeEvent event = new PropertyChangeEvent(this, EVT_CHANGED, null, List.copyOf(actions));
        listeners.forEach(listener -> listener.propertyChange(event));
    }
}
