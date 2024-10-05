package com.zyron.typewriter.text;

/**
 * Interface for listening to changes in an editable text object.
 *
 * Implementations of this interface can be registered with an {@link Editable} object to receive notifications
 * when text is inserted, appended, deleted, or replaced.
 */
public interface EditableListener {
    
    /**
     * Called when text is inserted into the editable object.
     *
     * @param offset The offset at which the text was inserted.
     * @param text The inserted text.
     */
    void onInserted(int offset, CharSequence text);

    /**
     * Called when text is appended to the end of the editable object.
     *
     * @param text The appended text.
     */
    void onAppended(CharSequence text);

    /**
     * Called when a range of text is deleted from the editable object.
     *
     * @param start The starting index of the deleted text.
     * @param end The ending index of the deleted text (exclusive).
     */
    void onDeleted(int start, int end);

    /**
     * Called when a range of text is replaced with new text.
     *
     * @param start The starting index of the replaced text.
     * @param end The ending index of the replaced text (exclusive).
     * @param text The replacement text.
     */
    void onReplaced(int start, int end, CharSequence text);
}