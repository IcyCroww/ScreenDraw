package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Класс для управления историей рисования (Undo/Redo)
 */
public class DrawingHistory {
    private final Stack<List<Stroke>> undoHistory = new Stack<>();
    private final Stack<List<Stroke>> redoHistory = new Stack<>();
    private static final int MAX_HISTORY = 50;

    /**
     * Сохраняет текущее состояние в историю
     */
    public void saveState(List<Stroke> currentStrokes) {
        // Создаем глубокую копию текущего состояния
        List<Stroke> stateCopy = new ArrayList<>();
        for (Stroke stroke : currentStrokes) {
            stateCopy.add(stroke.copy());
        }
        undoHistory.push(stateCopy);

        // Ограничиваем размер истории
        if (undoHistory.size() > MAX_HISTORY) {
            undoHistory.removeFirst();
        }

        // Очищаем redo при новом действии
        redoHistory.clear();
    }

    /**
     * Отменяет последнее действие
     */
    public List<Stroke> undo(List<Stroke> currentStrokes) {
        if (!undoHistory.isEmpty()) {
            // Сохраняем текущее состояние для redo
            List<Stroke> currentState = new ArrayList<>();
            for (Stroke stroke : currentStrokes) {
                currentState.add(stroke.copy());
            }
            redoHistory.push(currentState);

            // Восстанавливаем предыдущее состояние
            return undoHistory.pop();
        }
        return currentStrokes; // Возвращаем текущее состояние если нечего отменять
    }

    /**
     * Повторяет отмененное действие
     */
    public List<Stroke> redo(List<Stroke> currentStrokes) {
        if (!redoHistory.isEmpty()) {
            // Сохраняем текущее состояние для undo
            List<Stroke> currentState = new ArrayList<>();
            for (Stroke stroke : currentStrokes) {
                currentState.add(stroke.copy());
            }
            undoHistory.push(currentState);

            // Восстанавливаем состояние из redo
            return redoHistory.pop();
        }
        return currentStrokes; // Возвращаем текущее состояние если нечего повторять
    }

    /**
     * Проверяет, можно ли отменить действие
     */
    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    /**
     * Проверяет, можно ли повторить действие
     */
    public boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    /**
     * Получает размер истории отмен
     */
    public int getUndoHistorySize() {
        return undoHistory.size();
    }

    /**
     * Получает максимальный размер истории
     */
    public int getMaxHistorySize() {
        return MAX_HISTORY;
    }

    /**
     * Очищает всю историю
     */
    public void clear() {
        undoHistory.clear();
        redoHistory.clear();
    }
}