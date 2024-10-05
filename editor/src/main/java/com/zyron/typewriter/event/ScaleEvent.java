package com.zyron.typewriter.event;

import android.view.ScaleGestureDetector;
import com.zyron.typewriter.widget.CodeEditor;

public class ScaleEvent implements ScaleGestureDetector.OnScaleGestureListener {
    
    private final CodeEditor editor;
    private float lastFocusX;
    private float lastFocusY;

    public ScaleEvent(CodeEditor editor) {
        if (editor == null) {
            throw new IllegalArgumentException("CodeEditor cannot be null");
        }
        this.editor = editor;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();

        // Get the current text size and calculate the new size
        float currentTextSize = editor.getEditableTextPaint().getTextSize();
        float newSize = currentTextSize * scaleFactor;

        // Clamp the new size to desired limits
        if (newSize < 8 || newSize > 28) {
            return true; // Do not apply if out of bounds
        }

        // Get the focal point of the scaling gesture
        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();

        // Calculate the offset to keep the focal point in focus while zooming
        float dx = (focusX - lastFocusX) * (1 - scaleFactor);
        float dy = (focusY - lastFocusY) * (1 - scaleFactor);

        // Update the text size
        editor.setTextSize(newSize);

        // Scroll or translate the editor's content to keep the focal point centered
        editor.scrollBy((int) dx, (int) dy);

        // Redraw the editor to apply changes
        editor.invalidate();

        // Update the last focus points for the next onScale event
        lastFocusX = focusX;
        lastFocusY = focusY;

        return true; // Indicate that the scale event was handled
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        // Save the initial focal point when scaling begins
        lastFocusX = detector.getFocusX();
        lastFocusY = detector.getFocusY();
        return true; // Indicate that scaling should begin
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        // Optional: Implement any logic to execute when scaling ends
    }
}