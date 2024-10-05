package com.zyron.typewriter.event;

import androidx.annotation.NonNull;
import com.zyron.typewriter.widget.CodeEditor;

public class ScrollEvent extends Event {

    /**
     * Caused by thumb's exact movements
     */
    public final static int CAUSE_USER_DRAG = 1;
    /**
     * Caused by fling after user's movements
     */
    public final static int CAUSE_USER_FLING = 2;
    /**
     * Caused by calling {@link CodeEditor#ensurePositionVisible(int, int)}.
     * This can happen when this method is manually called or either the user edits the text
     */
    public final static int CAUSE_MAKE_POSITION_VISIBLE = 3;
    /**
     * Caused by the user's thumb reaching the edge of editor viewport, which causes the editor to
     * scroll to move the selection to text currently outside the viewport.
     */
    public final static int CAUSE_TEXT_SELECTING = 4;

    public final static int CAUSE_SCALE_TEXT = 5;

    private final int mStartX;
    private final int mStartY;
    private final int mEndX;
    private final int mEndY;
    private final int mCause;

    public ScrollEvent(@NonNull CodeEditor editor, int startX, int startY, int endX, int endY, int cause) {
        super(editor);
        mStartX = startX;
        mStartY = startY;
        mEndX = endX;
        mEndY = endY;
        mCause = cause;
    }

    /**
     * Get the start x
     */
    public int getStartX() {
        return mStartX;
    }

    /**
     * Get the start y
     */
    public int getStartY() {
        return mStartY;
    }

    /**
     * Get end x
     */
    public int getEndX() {
        return mEndX;
    }

    /**
     * Get end y
     */
    public int getEndY() {
        return mEndY;
    }

    /**
     * Get the cause of the scroll
     */
    public int getCause() {
        return mCause;
    }

}
