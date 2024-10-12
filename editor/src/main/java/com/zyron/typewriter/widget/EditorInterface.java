/**
 * Copyright 2024 Zyron Official.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zyron.typewriter.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.widget.EdgeEffect;
import com.zyron.typewriter.text.Editable;

/**
 * The EditorInterface class represents the editor user interface that is part of the CodeEditor class.
 * It implements the onDraw method to render the entire editor interface in small parts or methods.
 * It includes features like EditableString, Gutter with divider lines and line numbers, 
 * current line background, selection portion of the line background, and a cursor with a droplet.
 * 
 * @author Sheikh Abdul Aziz
 */
public class EditorInterface {

    public final CodeEditor editor;

    /**
     * Constructor for EditorInterface, which takes a CodeEditor instance to manage the editor's user interface.
     *
     * @param editor The constructor for EditorInterface, which takes a CodeEditor instance.
     */
    public EditorInterface(CodeEditor editor) {
        this.editor = editor;
    }

    /**
     * Draws the entire editor interface onto the provided Canvas object, handling multiple
     * components like background, line highlighting, and text.
     *
     * @param canvas The canvas on which the editor UI will be drawn.
     */
    public void onDraw(Canvas canvas) {
        drawEdgeEffect(canvas);
        drawSurfaceBackground(canvas);
        drawLineBackground(canvas);
        drawString(canvas);
        drawComponents(canvas);
    }
    
    /**
     * Draws the edge effects (e.g., overscroll or bounce effects) for the editor when scrolling
     * reaches the edges of the content.
     *
     * @param canvas The canvas on which the edge effects will be drawn.
     */
    private void drawEdgeEffect(Canvas canvas) {
        int getWidth = editor.getScreenWidth();
        int getHeight = editor.getScreenHeight();
        
        EdgeEffect edgeEffectLeft = editor.getHorizontalEdgeEffect();
        EdgeEffect edgeEffectRight = editor.getHorizontalEdgeEffect();
        EdgeEffect edgeEffectTop = editor.getVerticalEdgeEffect();
        EdgeEffect edgeEffectBottom = editor.getVerticalEdgeEffect();
        
        boolean needsInvalidate = false;

        if (!edgeEffectTop.isFinished()) {
            int restoreCount = canvas.save();
            canvas.translate(0f, 0f);
            edgeEffectTop.setSize(getWidth, getHeight);
            if (edgeEffectTop.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectBottom.isFinished()) {
            int restoreCount = canvas.save();
            canvas.translate(-getWidth, getHeight);
            edgeEffectBottom.setSize(getWidth, getHeight);
            canvas.rotate(180f, getWidth, 0f);
            if (edgeEffectBottom.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectLeft.isFinished()) {
            int restoreCount = canvas.save();
            canvas.translate(0f, getHeight);
            canvas.rotate(-90f, 0f, 0f);
            edgeEffectLeft.setSize(getHeight, getWidth);
            if (edgeEffectLeft.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectRight.isFinished()) {
            int restoreCount = canvas.save();
            canvas.translate(getWidth, 0f);
            canvas.rotate(90f, 0f, 0f);
            edgeEffectRight.setSize(getHeight, getWidth);
            if (edgeEffectRight.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (needsInvalidate) {
            editor.postInvalidateOnAnimation();
        }
    }    

    /**
     * Draws the background of the editor, including the gutter and editable area.
     *
     * @param canvas The canvas on which the background will be drawn.
     */
    private void drawSurfaceBackground(Canvas canvas) {
        drawGutterBackground(canvas);
        drawEditableBackground(canvas);
    }

    /**
     * Draws the background for the current line being edited to highlight it.
     *
     * @param canvas The canvas on which the line background will be drawn.
     */
    private void drawLineBackground(Canvas canvas) {
        drawCurrentLineBackground(canvas);
    }

    /**
     * Draws the text and line numbers within the gutter and editable area of the editor.
     *
     * @param canvas The canvas on which the text will be drawn.
     */
    private void drawString(Canvas canvas) {
        drawGutterInteger(canvas);
        drawEditableString(canvas);
    }

    /**
     * Draws various UI components such as the gutter divider, cursor, and selection droplets on the
     * editor interface.
     *
     * @param canvas The canvas on which the components will be drawn.
     */
    private void drawComponents(Canvas canvas) {
        drawGutterDividerLine(canvas);
        drawCursor(canvas);
        drawSelectionDroplet(canvas);
    }

    /**
     * Draws the background for the gutter, which can display line numbers or other information
     * along the left side of the editor.
     *
     * @param canvas The canvas on which the gutter background will be drawn.
     */
    private void drawGutterBackground(Canvas canvas) {
        boolean isGutterEnabled = editor.getGutterEnabled();
        int gutterWidth = editor.getGutterWidth();
        int width = editor.getScreenWidth() + editor.getMaxScrollX();
        int height = editor.getScreenHeight() + editor.getMaxScrollY();
        
        if (isGutterEnabled) {
        canvas.drawRect(0, 0, gutterWidth, height, editor.getGutterBackgroundPaint());
        }    
    }

    /**
     * Draws line numbers within the gutter, adjusting their position based on alignment settings.
     *
     * @param canvas The canvas on which the line numbers will be drawn.
     */
    private void drawGutterInteger(Canvas canvas) {
        boolean isGutterEnabled = editor.getGutterEnabled();
        int gutterWidth = editor.getGutterWidth();
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

    /**
     * Draws the divider line between the gutter and the editable text area.
     *
     * @param canvas The canvas on which the divider line will be drawn.
     */
    private void drawGutterDividerLine(Canvas canvas) {
        boolean isGutterEnabled = editor.getGutterEnabled();
        boolean isDividerLineEnabled = editor.getDividerLineEnabled();
        int gutterWidth = editor.getGutterWidth();
        
        if (isGutterEnabled) {
        if (isDividerLineEnabled) {
        canvas.drawLine(gutterWidth, 0, gutterWidth, editor.getScreenHeight() + editor.getScrollY(), editor.getGutterDividerLinePaint());     
        }
      }
    }

    /**
     * Draws the background for the editable text area of the editor.
     *
     * @param canvas The canvas on which the editable background will be drawn.
     */
    private void drawEditableBackground(Canvas canvas) {
        int gutterWidth = editor.getGutterWidth();
        canvas.drawRect(gutterWidth, 0, editor.getScreenWidth() + editor.getMaxScrollX(), editor.getScreenHeight() + editor.getMaxScrollY(), editor.getEditableBackgroundPaint());
    }

    /**
     * Draws the background for the current line, typically to highlight the line that contains the
     * cursor.
     *
     * @param canvas The canvas on which the current line background will be drawn.
     */
    private void drawCurrentLineBackground(Canvas canvas) {
        Paint.FontMetrics metrics = editor.getEditableTextPaint().getFontMetrics();
        float textLineHeightOffset = (editor.getLineHeight() - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent;
        float currentLineIndex = editor.getPaddingTop() + editor.getPaddingBottom();        
        boolean isSelectable = editor.getIsSelectable();
        boolean isReadOnly = editor.getIsReadOnly();
        boolean isEditable = editor.getIsEditable(); 
        int gutterWidth = editor.getGutterWidth();   
        int cursorPosX = editor.getCursorPosX();
        int cursorPosY = editor.getCursorPosY();        
        
        float topY = currentLineIndex * textLineHeightOffset;
        float bottomY = topY + editor.getLineHeight();

        if (!isSelectable || !isReadOnly) {
        canvas.drawRect(gutterWidth, topY + cursorPosY, editor.getScreenWidth() + editor.getScrollX(), bottomY + cursorPosY, editor.getCurrentLineBackgroundPaint());
        }
    }

    /**
     * Draws the background for the selected lines or text area within the editor.
     *
     * @param canvas The canvas on which the selection background will be drawn.
     */
    private void drawSelectionLineBackground(Canvas canvas) {
        boolean isSelectable = editor.getIsSelectable();
        int gutterWidth = editor.getGutterWidth();
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

    /**
     * Draws the editable text strings within the text area of the editor.
     *
     * @param canvas The canvas on which the text will be drawn.
     */
    private void drawEditableString(Canvas canvas) {
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

    /**
     * Draws the cursor within the text area, along with its optional "droplet" visual.
     *
     * @param canvas The canvas on which the cursor will be drawn.
     */
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
        
        float scaleFactorSizeX = 0.85f;
        float scaleFactorSizeY = 0.80f;
        int scaledDropletWidth = (int) (cursorDropletWidth * scaleFactorSizeX);
        int scaledDropletHeight = (int) (cursorDropletHeight * scaleFactorSizeY);

        if (isCursorVisible) {
            canvas.drawLine(cursorPosXStart + 5.5f, cursorPosYStart, cursorPosXEnd + 5.5f, cursorPosYEnd, cursorPaint);  
        }

        if (isCursorDropletVisible) {
            cursorDropletRes.setBounds((int)(cursorPosXStart + 5.5f - scaledDropletWidth / 2), cursorPosYStart + currentLineHeight, (int) (cursorPosXStart + 5.5f + scaledDropletWidth / 2), cursorPosYStart + currentLineHeight + scaledDropletHeight);
            cursorDropletRes.draw(canvas);
        }
    }

    /**
     * Draws the selection droplets that allow users to adjust the selected text range by dragging
     * the handles.
     *
     * @param canvas The canvas on which the selection droplets will be drawn.
     */
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