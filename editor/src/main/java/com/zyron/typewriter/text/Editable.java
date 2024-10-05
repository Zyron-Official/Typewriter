package com.zyron.typewriter.text;

import android.text.SpannableStringBuilder;
import android.util.Log;
import java.lang.CharSequence;

/**
 * A class representing editable text implements GapBuffer data structure. This class provides functionality for inserting, deleting, appending
 * replacing, and managing text selections. It also handles undo/redo operations and line
 * management, including measuring text and handling line breaks.
 */
public class Editable extends SpannableStringBuilder implements CharSequence {

    private EditableCache editableCache;
    private EditableStack editableStack;
    private EditableListener editableListener;

    private char[] editableContents;
    private int editableStartIndex;
    private int editableEndIndex;
    private int editableLineCount;
    private int editableLineLength;
    
    private final char BACKSPACE = '\b';
    private final char NEWLINE = '\n';
    private final char TAB = '\t';
    private final int EOF = '\uFFFF';

    private final int MINIMUM_TAB_SIZE = 2;    
    private final int DEFAULT_TAB_SIZE = 4;
    private final int MAXIMUM_TAB_SIZE = 8;

    private static final String TAG = "Editable";

    /**
     * Constructs an empty Editable object with an initial buffer size of 16 characters.
     */
    public Editable() {
        this(new char[16]);
    }

    /**
     * Constructs an Editable object with the specified CharSequence as initial content.
     *
     * @param editable The initial content of the Editable object.
     */
    public Editable(CharSequence editable) {
        if (editable != null) {
            insert(0, editable, false);
        }
    }

    /**
     * Constructs an Editable object with the specified character array as initial content.
     *
     * @param editable The initial content of the Editable object as a character array.
     */
    public Editable(char[] editable) {
        editableContents = (editable != null) ? editable : new char[16];
        editableLineCount = 1;
        editableLineLength = 0;
        editableStartIndex = 0;
        editableEndIndex = editableContents.length;
        editableCache = new EditableCache();
        editableStack = new EditableStack(this);

        for (char c : editableContents) {
            if (c == NEWLINE) {
                editableLineCount++;
            }
        }
    }

    /**
     * Sets a listener to be notified of changes to the Editable object.
     *
     * @param editableListener The listener to be notified of changes.
     */
    public void setEditableListener(EditableListener editableListener) {
        try {
            if (editableListener == null) {
                throw new IllegalArgumentException("listener can not be null");
            }
            this.editableListener = editableListener;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error setting listener: " + e.getMessage());
        }
    }

    /**
     * Inserts the specified CharSequence at the given offset in the Editable object.
     *
     * @param offset  The offset at which to insert the CharSequence.
     * @param str    The CharSequence to insert.
     * @param capture Whether to capture this operation in the undo stack.
     * @return This Editable object.
     */
    public synchronized Editable insert(int offset, CharSequence text, boolean capture) {
        return insert(offset, text, capture, System.nanoTime());
    }

    /**
     * Inserts the specified CharSequence at the given offset in the Editable object, capturing the
     * operation in the undo stack.
     *
     * @param offset The offset at which to insert the CharSequence.
     * @param str The CharSequence to insert.
     * @param capture Whether to capture this operation in the undo stack.
     * @param timestamp The timestamp of the insertion operation.
     * @return This Editable object.
     */
    public synchronized Editable insert(
            int offset, CharSequence text, boolean capture, long timestamp) {
        try {
            int length = text.length();
            if (capture && length > 0) {
                editableStack.captureInsert(offset, offset + length, timestamp);
            }

            int insertIndex = getRealIndex(offset);

            if (insertIndex != editableEndIndex) {
                if (isBeforeEditable(insertIndex)) {
                    shiftEditableLeft(insertIndex);
                } else {
                    shiftEditableRight(insertIndex);
                }
            }

            if (length >= editableSize()) {
                expandBuffer(length - editableSize());
            }

            for (int i = 0; i < length; ++i) {
                char c = text.charAt(i);
                if (c == BACKSPACE) {
                    --editableStartIndex;
                } else if (c == TAB) {
                    for (int j = 0; j < DEFAULT_TAB_SIZE; j++) {
                        editableContents[editableStartIndex] = ' ';
                        ++editableStartIndex;
                    }
                } else {
                    if (c == NEWLINE) {
                        ++editableLineCount;
                    }
                    editableContents[editableStartIndex] = c;
                    ++editableStartIndex;
                }
            }

            if (editableListener != null) {
                editableListener.onInserted(offset, text);
            }

            editableCache.invalidateCache(offset);
            return Editable.this;
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Index out of bounds: " + e.getMessage());
            return this;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument: " + e.getMessage());
            return this;
        }
    }

    /**
     * Appends the specified CharSequence to the end of the Editable object.
     *
     * @param str The CharSequence to append.
     * @param capture Whether to capture this operation in the undo stack.
     * @return This Editable object.
     */
    public synchronized Editable append(CharSequence text, boolean capture) {
        try {
            insert(length(), text, capture);
            return Editable.this;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument: " + e.getMessage());
            return this;
        }
    }

    /**
     * Appends the specified CharSequence to the end of the Editable object.
     *
     * @param str The CharSequence to append.
     * @return This Editable object.
     */
    public synchronized Editable append(CharSequence text) {
        try {
            insert(length(), text, false);
            if (editableListener != null) {
                editableListener.onAppended(text);
            }
            return Editable.this;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument: " + e.getMessage());
            return this;
        }
    }

    /**
     * Deletes the characters in the specified range from the Editable object.
     *
     * @param start The starting offset of the range to delete (inclusive).
     * @param end   The ending offset of the range to delete (exclusive).
     * @param capture Whether to capture this operation in the undo stack.
     * @return This Editable object.
     */
    public synchronized Editable delete(int start, int end, boolean capture) {
        return delete(start, end, capture, System.nanoTime());
    }

    /**
     * Deletes the characters in the specified range from the Editable object, capturing the
     * operation in the undo stack.
     *
     * @param start The starting offset of the range to delete (inclusive).
     * @param end   The ending offset of the range to delete (exclusive).
     * @param capture Whether to capture this operation in the undo stack.
     * @param timestamp The timestamp of the deletion operation.
     * @return This Editable object.
     */
    public synchronized Editable delete(int start, int end, boolean capture, long timestamp) {
        try {
            if (capture && start < end) {
                editableStack.captureDelete(start, end, timestamp);
            }

            int newEditableStart = end;

            if (newEditableStart != editableStartIndex) {
                if (isBeforeEditable(newEditableStart)) {
                    shiftEditableLeft(newEditableStart);
                } else {
                    shiftEditableRight(newEditableStart + editableSize());
                }
            }

            int len = end - start;
            for (int i = 0; i < len; ++i) {
                --editableStartIndex;
                if (editableContents[editableStartIndex] == NEWLINE) {
                    --editableLineCount;
                }
            }
            if (editableListener != null) {
                editableListener.onDeleted(start, end);
            }
            editableCache.invalidateCache(start);
            return Editable.this;
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Index out of bounds: " + e.getMessage());
            return this;
        }
    }

    /**
     * Replaces the characters in the specified range with the given CharSequence.
     *
     * @param start The starting offset of the range to replace (inclusive).
     * @param end   The ending offset of the range to replace (exclusive).
     * @param str   The CharSequence to replace the range with.
     * @param capture Whether to capture this operation in the undo stack.
     * @return This Editable object.
     */
    public synchronized Editable replace(int start, int end, CharSequence text, boolean capture) {
        try {
            delete(start, end, capture);
            insert(start, text, capture);
            if (editableListener != null) {
                editableListener.onReplaced(start, end, text);
            }
            return Editable.this;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument: " + e.getMessage());
            return this;
        }
    }

    /**
     * Returns a string of text corresponding to the line with the specified index.
     *
     * @param targetLineIndex The line index of interest (1-based).
     * @return The text on the specified line, or an empty string if the line does not exist.
     */
    public synchronized String getLineString(int targetLineIndex) {
        try {
            int startIndex = getLineStart(targetLineIndex);
            int endIndex = getLineLength(targetLineIndex);

            return substring(startIndex, startIndex + endIndex);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error getting line: " + e.getMessage());
            return "";
        }
    }

    /**
     * Gets the character offset of the first character of the line with the specified index. The
     * offset is counted from the beginning of the text.
     *
     * @param targetLineIndex The index of the line of interest.
     * @return The character offset of the line, or -1 if the line does not exist.
     */
    public synchronized int getLineStart(int targetLineIndex) {
        try {
            if (targetLineIndex <= 0 || targetLineIndex > getLineCount()) {
                throw new IllegalArgumentException("line index is invalid");
            }

            int currentLineIndex = --targetLineIndex;
            EditablePair<Integer, Integer> cacheEntry = editableCache.getNearestLine(currentLineIndex);
            int cacheLine = cacheEntry.first;
            int cacheOffset = cacheEntry.second;

            int offset = 0;
            if (currentLineIndex > cacheLine) {
                offset = getCharOffsetStart(currentLineIndex, cacheLine, cacheOffset);
            } else if (currentLineIndex < cacheLine) {
                offset = getCharOffsetEnd(currentLineIndex, cacheLine, cacheOffset);
            } else {
                offset = cacheOffset;
            }

            if (offset >= 0) {
                editableCache.updateEntry(currentLineIndex, offset);
            }
            return offset;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error getting line offset: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Gets the character offset of the end of the specified line.
     *
     * @param targetLineIndex The line index (1-based).
     * @return The character offset of the end of the line, or -1 if the line index is invalid.
     */
	public synchronized int getLineEnd(int targetLineIndex) {
		if(targetLineIndex < 0 || targetLineIndex >= getLineCount()){
			throw new IndexOutOfBoundsException("line index out of bounds");
		}
        
		if(targetLineIndex == getLineCount() - 1){
			return editableContents.length;
		}
		return getLineStart(targetLineIndex + 1);
	}    

    private int indexFirstOf(CharSequence s, int start, char c) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }
    
	private int indexLastOf(CharSequence s,int start,char c){
		for(int i = start; i >= 0; i--) {
			if(s.charAt(i)==c){
				return i;
			}
		}
		return -1;
	}
    
    /*
     * Precondition: startOffset is the offset of startLine
     */
    private int getCharOffsetStart(int targetLine, int startLine, int startOffset) {
        assert isValid(startOffset);

        int currLine = startLine;
        int offset = getRealIndex(startOffset);

        while ((currLine < targetLine) && (offset < editableContents.length)) {
            if (editableContents[offset] == NEWLINE) {
                ++currLine;
            }
            ++offset;

            if (offset == editableStartIndex) {
                offset = editableEndIndex;
            }
        }

        if (currLine != targetLine) {
            return -1;
        }
        return getLogicalIndex(offset);
    }

    /*
     * Precondition: endOffset is the offset of endLine
     */
    private int getCharOffsetEnd(int targetLine, int endLine, int endOffset) {
        assert isValid(endOffset);

        if (targetLine == 0) {
            return 0; 
        }

        int currLine = endLine;
        int offset = getRealIndex(endOffset);
        
        while (currLine > (targetLine - 1) && offset >= 0) {
            if (offset == editableEndIndex) {
                offset = editableStartIndex;
            }
            --offset;

            if (editableContents[offset] == NEWLINE) {
                --currLine;
            }
        }

        int charOffset;
        if (offset >= 0) {
            charOffset = getLogicalIndex(offset);
            ++charOffset;
        } else {
            charOffset = -1;
        }
        return charOffset;
    }
    
    /**
     * Moves `editableStartIndex` by `displacement` units.  A positive displacement moves
     * `editableStartIndex` to the right, while a negative displacement moves it to the left. This
     * method is intended for use by `UndoStack` only and does not perform error checking.
     *
     * @param displacement The number of units to move `editableStartIndex`.
     */
    public synchronized void shiftEditableStart(int displacement) {
        if (displacement >= 0)
            editableLineCount += getCountNewLines(editableStartIndex, displacement);
        else
            editableLineCount -= getCountNewLines(editableStartIndex + displacement, -displacement);

        editableStartIndex += displacement;
        editableCache.invalidateCache(getLogicalIndex(editableStartIndex - 1) + 1);
    }

    private int getCountNewLines(int start, int totalChars) {
        int newlines = 0;
        for (int i = start; i < (start + totalChars); ++i) {
            if (editableContents[i] == NEWLINE) {
                ++newlines;
            }
        }

        return newlines;
    }

    /**
     * Adjusts the gap in the `editableContents` array so that `editableStartIndex` is at
     * `newEditableStart`. This method is used to shift the gap to the left during deletion
     * operations.
     *
     * @param newEditableStart The new position for `editableStartIndex`.
     */
    private void shiftEditableLeft(int newEditableStart) {
        while (editableStartIndex > newEditableStart) {
            editableEndIndex--;
            editableStartIndex--;
            editableContents[editableEndIndex] = editableContents[editableStartIndex];
        }
    }

    /**
     * Adjusts the gap in the `editableContents` array so that `editableEndIndex` is at
     * `newEditableEnd`. This method is used to shift the gap to the right during insertion
     * operations.
     *
     * @param newEditableEnd The new position for `editableEndIndex`.
     */
    private void shiftEditableRight(int newEditableEnd) {
        while (editableEndIndex < newEditableEnd) {
            editableContents[editableStartIndex] = editableContents[editableEndIndex];
            editableStartIndex++;
            editableEndIndex++;
        }
    }

    /**
     * Gets the line number that charOffset is on.
     *
     * @param charOffset The character offset to find the line number for.
     * @return The line number that charOffset is on, or -1 if charOffset is invalid.
     */
    public synchronized int getLineOffset(int charOffset) {
        try {
            assert isValid(charOffset);

            EditablePair<Integer, Integer> cachedEntry = editableCache.getNearestCharOffset(charOffset);
            int line = cachedEntry.first;
            int offset = getRealIndex(cachedEntry.second);
            int targetOffset = getRealIndex(charOffset);
            int lastKnownLine = -1;
            int lastKnownCharOffset = -1;

            if (targetOffset > offset) {
                while ((offset < targetOffset) && (offset < editableContents.length)) {
                    if (editableContents[offset] == NEWLINE) {
                        ++line;
                        lastKnownLine = line;
                        lastKnownCharOffset = getLogicalIndex(offset) + 1;
                    }

                    ++offset;
                    if (offset == editableStartIndex) {
                        offset = editableEndIndex;
                    }
                }
            } else if (targetOffset < offset) {
                while ((offset > targetOffset) && (offset > 0)) {
                    if (offset == editableEndIndex) {
                        offset = editableStartIndex;
                    }
                    --offset;

                    if (editableContents[offset] == NEWLINE) {
                        lastKnownLine = line;
                        lastKnownCharOffset = getLogicalIndex(offset) + 1;
                        --line;
                    }
                }
            }

            if (offset == targetOffset) {
                if (lastKnownLine != -1) {
                    editableCache.updateEntry(lastKnownLine, lastKnownCharOffset);
                }
                return line + 1;
            } else {
                return 0;
            }
        } catch (AssertionError e) {
            Log.e(TAG, "Assertion error in getTargetLineIndex: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Finds the number of characters on the specified line. All valid lines contain at least one
     * character, which may be a non-printable one like '\n', '\t', or EOF.
     *
     * @param currentLineIndex The index of the line to find the length of (1-based).
     * @return The number of characters in the line, or 0 if the line does not exist.
     */
    public synchronized int getLineLength(int currentLineIndex) {
        try {
            int lineLength = 0;
            int pos = getLineStart(currentLineIndex);
            pos = getRealIndex(pos);

            while (pos < editableContents.length && editableContents[pos] != NEWLINE &&
                    editableContents[pos] != EOF) {
                ++lineLength;
                ++pos;
                if (pos == editableStartIndex) {
                    pos = editableEndIndex;
                }
            }
            return lineLength;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error getting line length: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Returns the total number of lines in the Editable object.
     *
     * @return The line count.
     */
    public synchronized int getLineCount() {
        return editableLineCount;
    }  

    /**
     * Gets the character at the specified offset. Does not perform bounds-checking.
     *
     * @param charOffset The offset of the character to retrieve.
     * @return The character at the offset, or '\0' (null character) if an error occurs.
     */
    @Override
    public synchronized char charAt(int charOffset) {
        try {
            return editableContents[getRealIndex(charOffset)];
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Array index out of bounds: " + e.getMessage());
            return '\0'; 
        }
    }

    /**
     * Gets a subsequence of characters from the Editable object, starting at the specified offset
     * and up to a maximum of `maxChars` characters.
     *
     * @param start The starting offset of the subsequence (inclusive).
     * @param end   The ending offset of the subsequence (exclusive).
     * @return The subsequence of characters, or an empty string if the offset is invalid or
     * `maxChars` is non-positive.
     */
    @Override
    public synchronized CharSequence subSequence(int start, int end) {
        try {
            assert isValid(start) && isValid(end);
            int count = end - start;
            if (end > length()) {
                count = length() - start;
            }
            int realIndex = getRealIndex(start);
            char[] chars = new char[count];

            for (int i = 0; i < count; ++i) {
                chars[i] = editableContents[realIndex];
                if (++realIndex == editableStartIndex) {
                    realIndex = editableEndIndex;
                }
            }
            return new String(chars);
        } catch (AssertionError e) {
            Log.e(TAG, "Assertion error in subSequence: " + e.getMessage());
            return "";
        }
    }

    /**
     * Returns a string representation of the characters between the specified start and end
     * offsets.
     *
     * @param start The starting offset (inclusive).
     * @param end   The ending offset (exclusive).
     * @return The substring between the start and end offsets.
     */
    public synchronized String substring(int start, int end) {
        return subSequence(start, end).toString();
    }

    /**
     * Gets `charCount` number of consecutive characters starting from `editableStartIndex`. This
     * method is intended for use by `UndoStack` only and does not perform error checking.
     *
     * @param charCount The number of characters to retrieve.
     * @return An array of characters, or an empty array if `charCount` is non-positive.
     */
    public char[] editableSubSequence(int charCount) {
        char[] chars = new char[charCount];

        for (int i = 0; i < charCount; ++i) {
            chars[i] = editableContents[editableStartIndex + i];
        }
        return chars;
    }
    
    /**
     * Sets the selection range in the Editable object.
     *
     * @param start The starting offset of the selection (inclusive).
     * @param end   The ending offset of the selection (exclusive).
     */
    public synchronized void setSelection(int start, int end) {
        if (!isValid(start) || !isValid(end)) {
            throw new IndexOutOfBoundsException("Invalid selection range: start=" + start + ", end=" + end);
        }

        if (start != editableStartIndex) {
            if (isBeforeEditable(start)) {
                shiftEditableLeft(start);
            } else {
                shiftEditableRight(start + editableSize());
            }
        }

        editableEndIndex = getRealIndex(end);
    }

    /**
     * Returns the starting offset of the current selection.
     *
     * @return The starting offset of the selection.
     */
    public synchronized int getSelectionStart() {
        return getLogicalIndex(editableStartIndex);
    }

    /**
     * Returns the ending offset of the current selection.
     *
     * @return The ending offset of the selection.
     */
    public synchronized int getSelectionEnd() {
        return getLogicalIndex(editableEndIndex);
    }

    /**
     * Returns the length of the current selection.
     *
     * @return The length of the selection.
     */
    public synchronized int getSelectionLength() {
        return getSelectionEnd() - getSelectionStart();
    }

    /**
     * Returns the text that is currently selected.
     *
     * @return The selected text as a string.
     */
    public synchronized String getSelectedText() {
        return substring(getSelectionStart(), getSelectionEnd());
    }

    /**
     * Expands the buffer (`editableContents` array) by at least `minIncrement` characters to
     * accommodate new insertions. The buffer size is doubled on each call to this method to avoid
     * frequent reallocations.
     *
     * @param minIncrement The minimum number of characters to increase the buffer by.
     */
    private void expandBuffer(int minIncrement) {
        int incrSize = Math.max(minIncrement, editableContents.length * 2 + 2);
        char[] temp = new char[editableContents.length + incrSize];
        assert temp.length <= Integer.MAX_VALUE;

        int i = 0;
        while (i < editableStartIndex) {
            temp[i] = editableContents[i];
            ++i;
        }

        i = editableEndIndex;
        while (i < editableContents.length) {
            temp[i + incrSize] = editableContents[i];
            ++i;
        }

        editableEndIndex += incrSize;
        editableContents = temp;
    }

    /**
     * Checks if the given character offset is valid (within the bounds of the Editable object).
     *
     * @param charOffset The character offset to check.
     * @return `true` if the offset is valid, `false` otherwise.
     */
    private boolean isValid(int charOffset) {
        return (charOffset >= 0 && charOffset <= this.length());
    }

    /**
     * Returns the size of the gap in the `editableContents` array.
     *
     * @return The number of characters in the gap.
     */
    private int editableSize() {
        return editableEndIndex - editableStartIndex;
    }

    /**
     * Converts a logical character offset to a real index in the `editableContents` array.
     *
     * @param index The logical character offset.
     * @return The corresponding real index in the array.
     */
    private int getRealIndex(int index) {
        if (isBeforeEditable(index)) return index;
        else return index + editableSize();
    }

    /**
     * Converts a real index in the `editableContents` array to a logical character offset.
     *
     * @param index The real index in the array.
     * @return The corresponding logical character offset.
     */
    private int getLogicalIndex(int index) {
        if (isBeforeEditable(index)) return index;
        else return index - editableSize();
    }

    /**
     * Checks if the given index is before the start of the gap in the `editableContents` array.
     *
     * @param index The index to check.
     * @return `true` if the index is before the gap, `false` otherwise.
     */
    private boolean isBeforeEditable(int index) {
        return index < editableStartIndex;
    }
    
    /**
     * Returns the total number of characters in the Editable object.
     *
     * @return The character count (length of the text).
     */
    @Override
    public synchronized int length() {
        return editableContents.length - editableSize();
    }

    /**
     * Returns a string representation of the Editable object.
     *
     * @return The string representation of the Editable object.
     */
    @Override
    public synchronized String toString() {
        StringBuffer buf = new StringBuffer();
        int len = this.length();
        for (int i = 0; i < len; i++) {
            buf.append(charAt(i));
        }
        return new String(buf);
    }

    /**
     * Checks if an undo operation is possible.
     *
     * @return `true` if an undo is possible, `false` otherwise.
     */
    public boolean isUndo() {
        return editableStack.isUndo();
    }

    /**
     * Checks if a redo operation is possible.
     *
     * @return `true` if a redo is possible, `false` otherwise.
     */
    public boolean isRedo() {
        return editableStack.isRedo();
    }

    /**
     * Performs an undo operation and returns the number of characters that were undone.
     *
     * @return The number of characters that were undone.
     */
    public int onUndo() {
        return editableStack.onUndo();
    }

    /**
     * Performs a redo operation and returns the number of characters that were redone.
     *
     * @return The number of characters that were redone.
     */
    public int onRedo() {
        return editableStack.onRedo();
    }

    /**
     * Begins a batch edit, grouping subsequent modifications into a single undo/redo unit.
     */
    public void beginBatchEdit() {
        editableStack.beginBatchEdit();
    }

    /**
     * Ends a batch edit, completing the grouping of modifications into a single undo/redo unit.
     */
    public void endBatchEdit() {
        editableStack.endBatchEdit();
    }

    /**
     * Checks if the Editable object is currently in a batch edit.
     *
     * @return `true` if in a batch edit, `false` otherwise.
     */
    public boolean isBatchEdit() {
        return editableStack.isBatchEdit();
    }
    
    /**
     * Handles BACKSPACE key press.
     * Deletes the character before the current selection, or the selected text if any.
     */
    public synchronized void toEnterBackspace() {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        if (start == end) { // No selection, delete the character before the cursor
            if (start > 0) {
                // Check if the character before the cursor is a tab
                if (charAt(start - 1) == TAB) {
                    // Delete the entire tab (equivalent to deleting tabWidth spaces)
                    delete(start - DEFAULT_TAB_SIZE, start, true); 
                } else {
                    delete(start - 1, start, true);
//                    insert(30, String.valueOf(BACKSPACE), true);
                }
            }
        } else { // Delete the selected text
            delete(start, end, true);
        }
    }
    
    /**
     * Handles NEWLINE key press.
     * Inserts a newline character at the current selection.
     */
    public synchronized void toEnterNewLine() {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        if (start == end) {
            // Insert a newline character at the current cursor position
            insert(0, String.valueOf(NEWLINE), true);
        } else {
            // Delete the selected text and insert a newline at the start of the selection
//            delete(start, end, true);
            insert(start, String.valueOf(NEWLINE), true);
        }
    }    

    /**
     * Handles TAB key press.
     * Inserts a tab character at the current selection, expanding it to spaces.
     */
    public synchronized void toEnterTab() {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        if (start == end) {
            insert(0, String.valueOf(TAB), true);
        } else {
            // Delete the selected text and insert a tab at the start of the selection
//            delete(start, end, true);
            insert(start, String.valueOf(TAB), true);
        }
    }
}