package com.zyron.typewriter.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

public class EditorInterface {

    private final CodeEditor editor;

    public EditorInterface(CodeEditor editor) {
        this.editor = editor;
    }

    public void onDraw(Canvas canvas) {
        int gutterWidth = editor.getGutterWidth();
        drawSurfaceBackground(canvas, gutterWidth);
        drawLineBackground(canvas, gutterWidth);
        drawString(canvas, gutterWidth);
        drawComponents(canvas, gutterWidth);
    }

    private void drawSurfaceBackground(Canvas canvas, int gutterWidth) {
        drawGutterBackground(canvas, gutterWidth);
        drawEditableBackground(canvas, gutterWidth);
    }

    private void drawLineBackground(Canvas canvas, int gutterWidth) {
        drawCurrentLineBackground(canvas, gutterWidth);
    }

    private void drawString(Canvas canvas, int gutterWidth) {
        drawGutterInteger(canvas, gutterWidth);
        drawEditableString(canvas);
    }

    private void drawComponents(Canvas canvas, int gutterWidth) {
        drawGutterDividerLine(canvas, gutterWidth);
        drawCursor(canvas);
        drawSelectionDroplet(canvas);
    }

    private void drawGutterBackground(Canvas canvas, int gutterWidth) {
        boolean isGutterEnabled = editor.getGutterEnabled();
        int width = editor.getScreenWidth() + editor.getMaxScrollX();
        int height = editor.getScreenHeight() + editor.getMaxScrollY();
        
        if (isGutterEnabled) {
        canvas.drawRect(0, 0, gutterWidth, height, editor.getGutterBackgroundPaint());
        }    
    }

    public void drawGutterInteger(Canvas canvas, int gutterWidth) {
        boolean isGutterEnabled = editor.getGutterEnabled();
        int startLine = Math.max(canvas.getClipBounds().top / editor.getLineHeight(), 1);
        int endLine = Math.min(canvas.getClipBounds().bottom / editor.getLineHeight() + 1, editor.getLineCount());    
        int lineHeight = editor.getLineHeight();

        Paint.FontMetrics metrics = editor.getGutterLineNumberPaint().getFontMetrics();
        float textBaselineOffset = (lineHeight - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent;

        for (int i = startLine; i <= endLine; i++) {
            int offsetX;
            switch (editor.getAlign()) {
                case LEFT:
                    offsetX = editor.getGutterPadding();
                    break;
                case CENTER:
                    offsetX = gutterWidth / 2;
                    break;
                case RIGHT:
                default:
                    offsetX = gutterWidth - editor.getGutterPadding();
                    break;
            }
            float offsetY = (i - 1) * lineHeight + textBaselineOffset;
            if (isGutterEnabled) {
            canvas.drawText(String.valueOf(i), offsetX, offsetY, editor.getGutterLineNumberPaint());
            }
        }
    }
    
    private void drawGutterDividerLine(Canvas canvas, int gutterWidth) {
        boolean isGutterEnabled = editor.getGutterEnabled();
        boolean isDividerLineEnabled = editor.getDividerLineEnabled();
        
        if (isGutterEnabled) {
        if (isDividerLineEnabled) {
        canvas.drawLine(gutterWidth, 0, gutterWidth, editor.getScreenHeight() + editor.getScrollY(), editor.getGutterDividerLinePaint());     
        }
      }
    }    

    private void drawEditableBackground(Canvas canvas, int gutterWidth) {
        canvas.drawRect(gutterWidth, 0, editor.getScreenWidth() + editor.getMaxScrollX(), editor.getScreenHeight() + editor.getMaxScrollY(), editor.getEditableBackgroundPaint());
    }

    private void drawCurrentLineBackground(Canvas canvas, int gutterWidth) {
        Paint.FontMetrics metrics = editor.getEditableTextPaint().getFontMetrics();
        float textLineHeightOffset = (editor.getLineHeight() - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent;
        float currentLineIndex = editor.getPaddingTop() + editor.getPaddingBottom();        
        boolean isSelectable = editor.getIsSelectable();
        boolean isReadOnly = editor.getIsReadOnly();
        boolean isEditable = editor.getIsEditable();    
        int cursorPosX = editor.getCursorPosX();
        int cursorPosY = editor.getCursorPosY();        
        
        float topY = currentLineIndex * textLineHeightOffset;
        float bottomY = topY + editor.getLineHeight();

        if (!isSelectable || !isReadOnly) {
        canvas.drawRect(gutterWidth, topY + cursorPosY, editor.getScreenWidth() + editor.getScrollX(), bottomY + cursorPosY, editor.getCurrentLineBackgroundPaint());
        }
    }
    
    private void drawSelectionLineBackground(Canvas canvas, int gutterWidth) {
        boolean isSelectable = editor.getIsSelectable();
        int left = editor.getGutterWidth() + editor.getEditablePadding();
        int lineHeight = editor.getLineHeight();
        int selectionDropletLeftX = editor.getSelectionDropletLeftX();
        int selectionDropletLeftY = editor.getSelectionDropletLeftY();
        int selectionDropletRightX = editor.getSelectionDropletRightX();
        int selectionDropletRightY = editor.getSelectionDropletRightY();
        
        int selectionStart = selectionDropletLeftY / lineHeight;
        int selectionEnd = selectionDropletRightY / lineHeight;
        
        if (selectionStart != selectionEnd) {
            for (int i = selectionStart; i <= selectionEnd; ++i) {
                int lineWidth = editor.getLineWidth(i) + editor.getWhiteSpaceWidth();
                if (i == selectionStart) {
                    canvas.drawRoundRect(selectionDropletLeftY, editor.getPaddingTop() + selectionDropletLeftY - lineHeight, left + lineWidth, editor.getPaddingTop() + selectionDropletLeftY, 5, 5, editor.getSelectionLineBackgroundPaint());
                } else if (i == selectionEnd) {
                    canvas.drawRoundRect(left, editor.getPaddingTop() + selectionDropletRightY - lineHeight, selectionDropletRightY, editor.getPaddingTop() + selectionDropletRightY, 5, 5, editor.getSelectionLineBackgroundPaint());
                } else {
                    canvas.drawRoundRect(left, editor.getPaddingTop() + (i - 1) * lineHeight, left + lineWidth, editor.getPaddingTop() + i * lineHeight, 5, 5, editor.getSelectionLineBackgroundPaint());
                }
            }
        } else if (selectionStart == selectionEnd) {
            canvas.drawRoundRect(selectionDropletLeftX, editor.getPaddingTop() + selectionDropletLeftY - lineHeight, selectionDropletRightX, editor.getPaddingTop() + selectionDropletRightY, 5, 5, editor.getSelectionLineBackgroundPaint());
        }        
    }

    public void drawEditableString(Canvas canvas) {
        Paint editablePaint = editor.getEditableTextPaint();
        TextPaint.FontMetrics metrics = editor.getEditableTextPaint().getFontMetrics();
        float textLineHeightOffset = (editor.getLineHeight() - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent;
        int startLine = Math.max(canvas.getClipBounds().top / editor.getLineHeight(), 1);
        int endLine = Math.min(canvas.getClipBounds().bottom / editor.getLineHeight() + 1, editor.getLineCount());

        for (int i = startLine; i <= endLine; i++) {
            String text = editor.getLine(i);
            float offsetX = editor.getGutterWidth() + editor.getEditablePadding();
            float offsetY = (i - 1) * editor.getLineHeight() + textLineHeightOffset;
            canvas.drawText(text, offsetX, offsetY, editablePaint);
        } 
    }
        
    private void drawCursor(Canvas canvas) {
        Drawable cursorDropletRes = editor.getCursorDropletRes();
        Paint cursorPaint = editor.getCursorPaint();
        int cursorPosXStart = editor.getCursorPosX();
        int cursorPosYStart = editor.getCursorPosY();
        int cursorPosXEnd = editor.getCursorPosX();
        int cursorPosYEnd = editor.getCursorPosY() + editor.getLineHeight();
        int cursorDropletWidth = editor.getCursorDropletWidth();
        int cursorDropletHeight = editor.getCursorDropletHeight();
        int currentLineHeight = editor.getLineHeight();
        boolean isCursorVisible = editor.getIsCursorVisible();
        boolean isCursorDropletVisible = editor.getIsCursorDropletVisible();

        if (isCursorVisible) {
            canvas.drawLine(cursorPosXStart + 5.5f, cursorPosYStart, cursorPosXEnd + 5.5f, cursorPosYEnd, cursorPaint);  
        }

        if (isCursorDropletVisible) {
            cursorDropletRes.setBounds(cursorPosXStart - cursorDropletWidth / 2, cursorPosYStart + currentLineHeight, cursorPosXStart + cursorDropletWidth / 2, cursorPosYStart + currentLineHeight + cursorDropletHeight);
            cursorDropletRes.draw(canvas);
        }
    }
    
    private void drawSelectionDroplet(Canvas canvas) {
        Drawable selectionDropletLeftRes = editor.getSelectionDropletLeftRes();
        Drawable selectionDropletRightRes = editor.getSelectionDropletRightRes();
        int selectionDropletWidth = editor.getSelectionDropletWidth();
        int selectionDropletHeight = editor.getSelectionDropletHeight();
        int selectionDropletLeftX = editor.getSelectionDropletLeftX();
        int selectionDropletLeftY = editor.getSelectionDropletLeftY();
        int selectionDropletRightX = editor.getSelectionDropletRightX();
        int selectionDropletRightY = editor.getSelectionDropletRightY();
        boolean isSelectable = editor.getIsSelectable();
        
        if(isSelectable) {
            selectionDropletLeftRes.setBounds(selectionDropletLeftX - selectionDropletWidth + selectionDropletWidth / 2, selectionDropletLeftY, selectionDropletLeftX + selectionDropletWidth / 2, selectionDropletLeftY + selectionDropletHeight);
            selectionDropletLeftRes.draw(canvas);

            selectionDropletRightRes.setBounds(selectionDropletRightX - selectionDropletWidth / 2, selectionDropletRightY, selectionDropletRightX + selectionDropletWidth - selectionDropletWidth / 2, selectionDropletRightY + selectionDropletHeight);
            selectionDropletRightRes.draw(canvas);
        }
    }
}