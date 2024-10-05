package com.zyron.typewriter.event;

import androidx.annotation.NonNull;

import java.util.Objects;

import com.zyron.typewriter.widget.CodeEditor;

public abstract class Event {

    private final long mEventTime;
    private final CodeEditor mEditor;
    private int mInterceptTargets;

    public Event(@NonNull CodeEditor editor) {
        this(editor, System.currentTimeMillis());
    }

    public Event(@NonNull CodeEditor editor, long eventTime) {
        mEditor = Objects.requireNonNull(editor);
        mEventTime = eventTime;
        mInterceptTargets = 0;
    }

    /**
     * Get event time
     */
    public long getEventTime() {
        return mEventTime;
    }

    /**
     * Get the editor
     */
    @NonNull
    public CodeEditor getEditor() {
        return mEditor;
    }

    /**
     * Check whether this event can be intercepted (so that the event is not sent to other
     * receivers after being intercepted)
     * Intercept-able events:
     *
     * @see LongPressEvent
     * @see ClickEvent
     * @see DoubleClickEvent
     * @see EditorKeyEvent
     */
    public boolean canIntercept() {
        return false;
    }

    /**
     * Intercept the event for all targets.
     * <p>
     * Make sure {@link #canIntercept()} returns true. Otherwise, an {@link UnsupportedOperationException}
     * will be thrown.
     *
     * @see InterceptTarget
     */
    public void intercept() {
        if (!canIntercept()) {
            throw new UnsupportedOperationException("intercept() not supported");
        }
        mInterceptTargets = InterceptTarget.TARGET_EDITOR.getValue() | InterceptTarget.TARGET_RECEIVERS.getValue();
    }

    /**
     * Intercept the event for some targets
     *
     * @param targets Masks for target types
     * @see InterceptTarget
     */
    public void intercept(int targets) {
        if (!canIntercept()) {
            throw new UnsupportedOperationException("intercept() not supported");
        }
        mInterceptTargets = targets;
    }

    /**
     * Get intercepted dispatch targets
     *
     * @see #intercept(int)
     * @see InterceptTarget
     */
    public int getInterceptTargets() {
        return mInterceptTargets;
    }

    /**
     * Check whether this event is intercepted for some types of targets
     *
     * @see #getInterceptTargets()
     */
    public boolean isIntercepted() {
        return mInterceptTargets != 0;
    }

}
enum InterceptTarget {
    TARGET_EDITOR(0x01), // Example bitmask for editor
    TARGET_RECEIVERS(0x02); // Example bitmask for receivers

    private final int value;

    InterceptTarget(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}