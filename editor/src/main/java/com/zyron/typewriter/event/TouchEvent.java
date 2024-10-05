package com.zyron.typewriter.event;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.zyron.typewriter.widget.CodeEditor;

public final class TouchEvent {

    private final CodeEditor editor;

    public TouchEvent(CodeEditor editor) {
        this.editor = editor;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_CANCEL:
            editor.getVerticalEdgeEffect().onRelease();
            editor.getHorizontalEdgeEffect().onRelease();
            editor.invalidate();
                break;
            default:
                break;
        }
        return true;
    }
}