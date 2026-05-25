package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class DrawingHistory {
    private final Deque<List<Stroke>> undoHistory = new ArrayDeque<>();
    private final Deque<List<Stroke>> redoHistory = new ArrayDeque<>();
    private static final int MAX_HISTORY = 50;

    public void saveState(List<Stroke> currentStrokes) {
        undoHistory.addLast(copyStrokes(currentStrokes));

        if (undoHistory.size() > MAX_HISTORY) {
            undoHistory.removeFirst();
        }

        redoHistory.clear();
    }

    public List<Stroke> undo(List<Stroke> currentStrokes) {
        if (!undoHistory.isEmpty()) {
            redoHistory.addLast(copyStrokes(currentStrokes));
            return undoHistory.removeLast();
        }
        return currentStrokes;
    }

    public List<Stroke> redo(List<Stroke> currentStrokes) {
        if (!redoHistory.isEmpty()) {
            undoHistory.addLast(copyStrokes(currentStrokes));
            return redoHistory.removeLast();
        }
        return currentStrokes;
    }

    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    public int getUndoHistorySize() {
        return undoHistory.size();
    }

    public int getMaxHistorySize() {
        return MAX_HISTORY;
    }

    public void clear() {
        undoHistory.clear();
        redoHistory.clear();
    }

    private static List<Stroke> copyStrokes(List<Stroke> strokes) {
        List<Stroke> stateCopy = new ArrayList<>();
        for (Stroke stroke : strokes) {
            stateCopy.add(stroke.copy());
        }
        return stateCopy;
    }
}
