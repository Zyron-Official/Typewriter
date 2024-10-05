package com.zyron.typewriter.event;

import android.content.Context;
import android.content.ClipboardManager;
import android.os.Build;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.zyron.typewriter.event.ScrollEvent;
import com.zyron.typewriter.text.Editable;
import com.zyron.typewriter.view.TextInputConnection;
import com.zyron.typewriter.widget.CodeEditor;

public class GestureEvent implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private final CodeEditor editor;
    private final TextInputConnection textInput;
    private boolean isVerticalEdgeEffect;
    private boolean isHorizontalEdgeEffect;
    private boolean touchOnSelectionDroplet = false;
    private boolean touchOnSelectionDropletLeft = false;
    private boolean touchOnSelectionDropletRight = false;

    public GestureEvent(CodeEditor editor) {
        if (editor == null) {
            throw new IllegalArgumentException("CodeEditor cannot be null");
        }
        this.editor = editor;
        this.textInput = editor.getTextInput();
        
    }

    private Runnable moveAction = new Runnable() {
        @Override
        public void run() {
            editor.onMove(editor.getWhiteSpaceWidth() * 4, editor.getLineHeight());
            editor.postDelayed(moveAction, editor.getDefaultDuration());
        }
    };

    private String findNearestWord() {
        int length = editor.getEditable().length();
        Editable editableText = editor.getEditable();
        int selectionStart = editor.getSelectionStart();
        int selectionEnd = editor.getSelectionEnd(); 
        int cursorIndex = editor.getCursorIndex();  

        // select start index
        for (selectionStart = cursorIndex; selectionStart >= 0; --selectionStart) {
            char c = editableText.charAt(selectionStart);
            if (!Character.isJavaIdentifierPart(c)) break;
        }

        // select end index
        for (selectionEnd = cursorIndex; selectionEnd < length; ++selectionEnd) {
            char c = editableText.charAt(selectionEnd);
            if (!Character.isJavaIdentifierPart(c)) break;
        }

        // select start index needs to be incremented by 1
        ++selectionStart;
        if (selectionStart < selectionEnd)
            return editableText.substring(selectionStart, selectionEnd);
        return null;
    }

    private void reverse() {
        int selectionDropletLeftX = editor.getSelectionDropletLeftX();
        int selectionDropletLeftY = editor.getSelectionDropletLeftY();
        int selectionDropletRightX = editor.getSelectionDropletRightX();
        int selectionDropletRightY = editor.getSelectionDropletRightY();
        
        int selectionStart = editor.getSelectionStart();
        int selectionEnd = editor.getSelectionEnd();        
        
        selectionDropletLeftX = selectionDropletLeftX ^ selectionDropletRightX;
        selectionDropletRightX = selectionDropletLeftX ^ selectionDropletRightX;
        selectionDropletLeftX = selectionDropletLeftX ^ selectionDropletRightX;

        selectionDropletLeftY = selectionDropletLeftY ^ selectionDropletRightY;
        selectionDropletRightY = selectionDropletLeftY ^ selectionDropletRightY;
        selectionDropletLeftY = selectionDropletLeftY ^ selectionDropletRightY;

        selectionStart = selectionStart ^ selectionEnd;
        selectionEnd = selectionStart ^ selectionEnd;
        selectionStart = selectionStart ^ selectionEnd;

        touchOnSelectionDropletLeft = !touchOnSelectionDropletLeft;
        touchOnSelectionDropletRight = !touchOnSelectionDropletRight;
    }

    private boolean checkSelectRange(float x, float y) {

        if (y < editor.getSelectionDropletLeftY() - editor.getLineHeight()
                || y > editor.getSelectionDropletRightY()) return false;

        // on the same line
        if (editor.getSelectionDropletLeftY() == editor.getSelectionDropletRightY()) {
            if (x < editor.getSelectionDropletLeftY() || x > editor.getSelectionDropletRightX())
                return false;
        } else {
            // not on the same line
            int left = editor.getGutterWidth();
            int line = (int) y / editor.getLineHeight() + 1;
            int width = editor.getLineWidth(line) + editor.getWhiteSpaceWidth();
            // select start line
            if (line == editor.getSelectionDropletLeftY() / editor.getLineHeight()) {
                if (x < editor.getSelectionDropletLeftX() || x > left + width) return false;
            } else if (line == editor.getSelectionDropletRightY() / editor.getLineHeight()) {
                // select end line
                if (x < left || x > editor.getSelectionDropletRightX()) return false;
            } else {
                if (x < left || x > left + width) return false;
            }
        }
        return true;
    }

    public void onUp(MotionEvent event) {
        int selectionStart = editor.getSelectionStart();
        int selectionEnd = editor.getSelectionEnd();
        
        long lastTapTime = editor.getLastTapTime();
        Runnable blinkAction = editor.getBlinkAction();

        if (touchOnSelectionDropletLeft
                || touchOnSelectionDropletRight
                || touchOnSelectionDroplet) {
            editor.removeCallbacks(moveAction);

            touchOnSelectionDroplet = false;
            touchOnSelectionDropletLeft = false;
            touchOnSelectionDropletRight = false;

            if (editor.getIsSelectable()) {
                editor.setCursorPositionByCoordinate(selectionStart, selectionEnd);
            } else {
                lastTapTime = System.currentTimeMillis();
                editor.postDelayed(blinkAction, editor.getBlinkTimeout());
            }
        }
    }

    @Override
    public boolean onDown(MotionEvent event) {
        // Get the coordinates of the touch event
        float x = event.getX();
        float y = event.getY();

        // Check if the touch is within the left or right selection droplet area
//        if (editor.isTouchOnLeftSelectionDroplet(x, y)) {
//            touchOnSelectionDropletLeft = true;
//            editor.removeCallbacks(editor.getBlinkAction()); // Stop the cursor blinking
//            return true;
//        } else if (editor.isTouchOnRightSelectionDroplet(x, y)) {
//            touchOnSelectionDropletRight = true;
//            editor.removeCallbacks(editor.getBlinkAction()); // Stop the cursor blinking
//            return true;
//        } else if (editor.isTouchOnSelectionDroplet(x, y)) {
//            touchOnSelectionDroplet = true;
//            editor.removeCallbacks(editor.getBlinkAction()); // Stop the cursor blinking
//            return true;
//        }

        // If touch is not on any selection droplets, continue with regular down action
        editor.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY); // Provide haptic feedback on touch
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        // Optional: Implement onShowPress behavior here.
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        float x = event.getX() + editor.getScrollX();
        float y = event.getY() + editor.getScrollY();
        boolean isSelectable = editor.getIsSelectable();
        boolean isCursorVisible = editor.getIsCursorVisible();
        boolean isCursorDropletVisible = editor.getIsCursorDropletVisible();
        Runnable blinkAction = editor.getBlinkAction();
        long lastTapTime = editor.getLastTapTime();

            editor.onShowSoftKeyInput();
            editor.requestFocus();

            if(!isSelectable || !checkSelectRange(x, y)) {
                // stop cursor blink
                editor.removeCallbacks(blinkAction);
            
                isCursorVisible = isCursorDropletVisible = true;
                isSelectable = false;

                if(!editor.getReplaceList().isEmpty()) 
                    editor.getReplaceList().clear();

                editor.setCursorPositionByCoordinate(x, y);
                //Log.i(TAG, "mCursorIndex: " + mCursorIndex);
                editor.invalidate();
                lastTapTime = System.currentTimeMillis();
                // cursor start blink
                editor.postDelayed(blinkAction, editor.getBlinkTimeout());
            }         
        return true; // Event handled.
    }
    
    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        // Optional: Handle confirmed single tap event.
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        // Optional: Handle double-tap event.
        
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        // Optional: Handle events in a double-tap gesture.
        return true;
    }    
    
    @Override
    public void onLongPress(MotionEvent event) {
        // Optional: Implement long-press behavior here.
        editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        int currX = editor.getScroller().getCurrX();
        int currY = editor.getScroller().getCurrY();

        // Calculate the maximum scroll limits
        int maxScrollX =  editor.getMaxScrollX();
        int maxScrollY =  editor.getMaxScrollY();

        // Calculate the new end positions
        int endX = currX + (int) distanceX;
        int endY = currY + (int) distanceY;

        // Clamp the X scrolling between 0 and maxScrollX
        if (endX > maxScrollX) {
            distanceX = maxScrollX - currX;
        } else if (endX < 0) {
            distanceX = currX;
        }

        // Clamp the Y scrolling between 0 and maxScrollY
        if (endY > maxScrollY) {
            distanceY = maxScrollY - currY;
        } else if (endY < 0) {
            distanceY = currY;
        }

        // Perform the scroll using the updated distances
        editor.getScroller().startScroll(currX, currY, (int) distanceX, (int) distanceY, 0);
        editor.invalidate();

        return true;
    }
    
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        if (Math.abs(velocityY) > Math.abs(velocityX)) {
            velocityX = 0;
        } else {
            velocityY = 0;
        }

        // Calculate the correct x and y bounds
        int minX = 0;
        int maxX = editor.getMaxScrollX();
        int minY = 0;
        int maxY = editor.getMaxScrollY();

        // Perform the fling within the scroll bounds
        editor.getScroller().fling(editor.getScrollX(), editor.getScrollY(), (int) -velocityX, (int) -velocityY, minX, maxX, minY, maxY, 20, 20);
        editor.invalidate();

        return true;
    }
}