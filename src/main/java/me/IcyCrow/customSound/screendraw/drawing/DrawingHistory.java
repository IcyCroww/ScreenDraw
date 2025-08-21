package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawingHistory {
    private final Stack<List<Stroke>> undoHistory = new Stack<>();
    private final Stack<List<Stroke>> redoHistory = new Stack<>();
    private static final int MAX_HISTORY = 50;


    public void saveState(List<Stroke> currentStrokes) {
        List<Stroke> stateCopy = new ArrayList<>();
        for (Stroke stroke : currentStrokes) {
            stateCopy.add(stroke.copy());
        }
        undoHistory.push(stateCopy);

        if (undoHistory.size() > MAX_HISTORY) {
            undoHistory.removeFirst();
        }

        redoHistory.clear();
    }

    public List<Stroke> undo(List<Stroke> currentStrokes) {
        if (!undoHistory.isEmpty()) {
            List<Stroke> currentState = new ArrayList<>();
            for (Stroke stroke : currentStrokes) {
                currentState.add(stroke.copy());
            }
            redoHistory.push(currentState);

            return undoHistory.pop();
        }
        return currentStrokes;
    }

    public List<Stroke> redo(List<Stroke> currentStrokes) {
        if (!redoHistory.isEmpty()) {
            List<Stroke> currentState = new ArrayList<>();
            for (Stroke stroke : currentStrokes) {
                currentState.add(stroke.copy());
            }
            undoHistory.push(currentState);

            return redoHistory.pop();
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
}