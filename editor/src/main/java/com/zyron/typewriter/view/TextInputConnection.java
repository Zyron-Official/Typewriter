package com.zyron.typewriter.view;

import android.text.Selection;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;

import com.zyron.typewriter.text.Editable;
import com.zyron.typewriter.widget.CodeEditor;

/**
 * TextInputConnection class manages key events and text input for the CodeEditor widget.
 */
public class TextInputConnection extends BaseInputConnection {

    private final CodeEditor editor;
    private final Editable editableText;
    private boolean isEditable = true;

    // Special key constants
    private final String BACKSPACE = "\b";
    private final String NEWLINE = "\n";
    private final String TAB = "\t";

    public TextInputConnection(CodeEditor editor) {
        super(editor, true);
        if (editor == null) {
            throw new IllegalArgumentException("CodeEditor cannot be null");
        }
        this.editor = editor;
        this.editableText = editor.getEditable();
    }

    /**
     * Handles key events for the editor.
     *
     * @param event The key event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {

                // Handle Enter, Delete, and Tab keys
                case KeyEvent.KEYCODE_ENTER:
                    editor.onInsert(NEWLINE);
                    return true;

                case KeyEvent.KEYCODE_DEL:
                    editor.onDelete();
                    return true;

                case KeyEvent.KEYCODE_TAB:
                    editor.onInsert(TAB);
                    return true;

                // Navigation and escape key handling
                case KeyEvent.KEYCODE_ESCAPE:
                    return true;

                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MOVE_HOME:
                case KeyEvent.KEYCODE_MOVE_END:
                    return true;

                // Handle Ctrl+Keyboard shortcuts
                case KeyEvent.KEYCODE_C:
                    if (event.isCtrlPressed()) {
                        editor.onCopy();
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_P:
                    if (event.isCtrlPressed()) {
                        editor.onPaste();
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_X:
                    if (event.isCtrlPressed()) {
                        editor.onCut();
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_A:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement select all
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_O:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement open file
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_Z:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement undo
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_Y:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement redo
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_F:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement find/search
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_R:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement replace
                        return true;
                    }
                    break;

                case KeyEvent.KEYCODE_E:
                    if (event.isCtrlPressed()) {
                        // TODO: Implement exit/shutdown
                        return true;
                    }
                    break;

                // Close tabs (current, all, other)
                case KeyEvent.KEYCODE_W:
                    if (event.isCtrlPressed()) {
                        if (event.isShiftPressed()) {
                            // TODO: Implement close all tabs
                            editor.IsEditable();
                        } else if (event.isAltPressed()) {
                            // TODO: Implement close other tabs
                            editor.IsEditable();
                        } else {
                            // TODO: Implement close current tab
                            editor.IsEditable();
                        }
                        return true;
                    }
                    break;

                default:
                    break;
            }
        }
        return super.sendKeyEvent(event);
    }

    // ------------------ Composing text methods ------------------

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        if (text != null) {
            int composingStart = Selection.getSelectionStart(editableText);
            int composingEnd = Selection.getSelectionEnd(editableText);

            if (composingStart == composingEnd) {
                editor.onInsert(text);  // Insert composing text
            } else {
                editableText.replace(composingStart, composingEnd, text);  // Replace composing text
                editor.onInsert(editableText);
            }

            Selection.setSelection(editableText, newCursorPosition);  // Update cursor position
            editor.invalidate();
        }
        return true;
    }

    @Override
    public boolean finishComposingText() {
        editor.invalidate();  // Commit composing text
        return super.finishComposingText();
    }

    // ------------------ Text retrieval methods ------------------

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        int start = Math.max(Selection.getSelectionStart(editableText) - length, 0);
        int end = Selection.getSelectionStart(editableText);
        return editableText.subSequence(start, end);
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        int start = Selection.getSelectionEnd(editableText);
        int end = Math.min(start + length, editableText.length());
        return editableText.subSequence(start, end);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        int start = Selection.getSelectionStart(editableText);
        int end = Selection.getSelectionEnd(editableText);
        if (start == end) {
            return null;
        }
        return editableText.subSequence(start, end);
    }

    // ------------------ Text editing methods ------------------

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        editor.onInsert(text);
        editor.invalidate();
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return super.deleteSurroundingText(beforeLength, afterLength);
    }

    // ------------------ Cursor and selection methods ------------------

    @Override
    public boolean setSelection(int start, int end) {
        return super.setSelection(start, end);
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        return true;
    }

    // ------------------ Batch edit methods ------------------

    @Override
    public boolean beginBatchEdit() {
        return super.beginBatchEdit();
    }

    @Override
    public boolean endBatchEdit() {
        return super.endBatchEdit();
    }
}