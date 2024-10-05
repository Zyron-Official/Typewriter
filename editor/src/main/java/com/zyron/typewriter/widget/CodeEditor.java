package com.zyron.typewriter.widget;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.text.MeasuredText;
import android.icu.util.Measure;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.text.InputType;
import android.text.Selection;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorBoundsInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.view.inputmethod.EditorInfoCompat;

import com.zyron.typewriter.R;
import com.zyron.typewriter.event.GestureEvent;
import com.zyron.typewriter.event.ScaleEvent;
import com.zyron.typewriter.event.ScrollEvent;
import com.zyron.typewriter.event.TouchEvent;
import com.zyron.typewriter.text.Editable;
import com.zyron.typewriter.text.EditableListener;
import com.zyron.typewriter.util.DisplayUtils;
import com.zyron.typewriter.view.TextInputConnection;

import java.util.ArrayList;

public class CodeEditor extends View implements EditableListener {

    private CodeEditor codeEditor;
    private Editable editableText;
    private EditableListener editableListener;
    private EditorInterface editorInterface;
    private TouchEvent touchEvent;
    private GestureEvent gestureEvent;
    private RectF verticalScrollBarRect;
    private RectF horizontalScrollBarRect; 
    private EdgeEffect edgeEffectVertical;
    private EdgeEffect edgeEffectHorizontal;          
    private TextInputConnection textInputConnection;
    private InputMethodManager inputMethodManager;
    private InputMethodService inputMethodService;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OverScroller scroller;
    private ClipboardManager clipboard;
    private ArrayList replaceList;
    private Runnable blinkAction;

    // Paints
    private Paint editableBackgroundPaint;
    private TextPaint editableTextPaint;
    private Paint gutterBackgroundPaint;
    private Paint gutterDividerLinePaint;
    private Paint gutterLineNumberPaint;
    private Paint currentLineBackgroundPaint;
    private Paint selectionLineBackgroundPaint;
    private Paint cursorPaint;

    private Typeface typeface;
    private Paint.Align align;
    
    private Drawable horizontalScrollbarThumbDrawable;
    private Drawable horizontalScrollbarTrackDrawable;
    private Drawable verticalScrollbarThumbDrawable;
    private Drawable verticalScrollbarTrackDrawable;    
    private Drawable cursorDropletRes;    
    private Drawable selectionDropletLeftRes;
    private Drawable selectionDropletRightRes;
    
    private int cursorPositionX, cursorPositionY;
    private int cursorLine, cursorIndex;
    private int cursorWidth, cursorHeight; 
    
    private int screenWidth, screenHeight;
    private int maxScrollX, maxScrollY;
    private int lineWidth, spaceWidth;
    private int cursorDropletWidth, cursorDropletHeight;
    private int selectionStart, selectionEnd;

    private int selectionDropletWidth, selectionDropletHeight;
    private int selectionDropletLeftX, selectionDropletLeftY;
    private int selectionDropletRightX, selectionDropletRightY;
    
    private int whiteSpaceWidth;
    
    private boolean isGutterEnabled = true;
    private boolean isDividerLineEnabled = true;  
    private boolean isWordwrapEnabled = false;
    private boolean isMagnifierEnabled = true;       
    private boolean isCursorVisible = true;  
    private boolean isCursorDropletVisible = true;   
    private boolean isEditable = true;
    private boolean isReadOnly = false;
    private boolean isSoftKey = true;
    private boolean isHardKey = false;
    private boolean isSelectable = false;   
    private boolean isGutterPinned = false;
    private boolean verticalScrollBarEnabled;
    private boolean horizontalScrollBarEnabled;
    private boolean isHapticEnabled, isKeyHapticEnabled;
    
    private float velocityY;
    private String defaultText;
    private long lastScroll;
    // record last single tap time
    private long lastTapTime;
    // left margin for draw text
    private final int SPACEING = 0;
    // animation duration 250ms
    private final int DEFAULT_DURATION = 250;
    // cursor blink BLINK_TIMEOUT 500ms
    private final int BLINK_TIMEOUT = 500;    

    // Default values
    private static final int GUTTER_PADDING = 10;
    private static final int EDITABLE_PADDING = 5;
    private static final int MINIMUM_TEXT_SIZE_SP = 10;
    private static final int MAXIMUM_TEXT_SIZE_SP = 28;
    private static final int DEFAULT_TEXT_SIZE_SP = 10;  
    
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_PASTE = 2;
    private static final int MENU_ITEM_CUT = 3;
  
    
    public CodeEditor(Context context) {
        this(context, null);
    }

    public CodeEditor(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodeEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CodeEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        editableText = new Editable();
        editorInterface = new EditorInterface(this);
        touchEvent = new TouchEvent(this);  
        gestureEvent = new GestureEvent(this);
        scroller = new OverScroller(context);
        clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        replaceList = new ArrayList<>();  
        verticalScrollBarRect = new RectF();
        horizontalScrollBarRect = new RectF();      
        edgeEffectVertical = new EdgeEffect(getContext());
        edgeEffectHorizontal = new EdgeEffect(getContext());
//        textInputConnection = new TextInputConnection(this);
        inputMethodManager  = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        inputMethodService = (InputMethodService) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        codeEditor = new CodeEditor(context);
//        inputMethodService.setInputView(codeEditor);
        gestureDetector = new GestureDetector(context, new GestureEvent(this));
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleEvent(this));
        
        screenWidth = DisplayUtils.getScreenWidth(context);
        screenHeight = DisplayUtils.getScreenHeight(context);
        
        cursorLine = getLineCount();

        selectionDropletLeftRes = context.getDrawable(R.drawable.ic_droplet);
        selectionDropletLeftRes.setTint(Color.parseColor("#0B57FF"));

//        selectionDropletWidth = selectionDropletLeftRes.getIntrinsicWidth();
//        selectionDropletHeight = selectionDropletLeftRes.getIntrinsicHeight();

        selectionDropletRightRes = context.getDrawable(R.drawable.ic_droplet);
        selectionDropletRightRes.setTint(Color.parseColor("#0B57FF"));

        cursorDropletRes = context.getDrawable(R.drawable.ic_droplet);
        cursorDropletRes.setTint(Color.parseColor("#0B57FF"));
        
//        cursorDropletWidth = cursorDropletRes.getIntrinsicWidth();
//        cursorDropletHeight = cursorDropletRes.getIntrinsicHeight();        

        // Initialize Paint objects and Typeface
        editableBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        editableTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        gutterBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gutterDividerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gutterLineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentLineBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionLineBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Set default colors
        editableBackgroundPaint.setColor(Color.parseColor("#FFFFFF"));
        editableTextPaint.setColor(Color.parseColor("#606060"));
        gutterBackgroundPaint.setColor(Color.parseColor("#F2F2F2"));
        gutterDividerLinePaint.setColor(Color.parseColor("#9E9E9E"));
        gutterLineNumberPaint.setColor(Color.parseColor("#606060"));
        currentLineBackgroundPaint.setColor(Color.parseColor("#D8E3FF"));
        selectionLineBackgroundPaint.setColor(Color.parseColor("#D8E3FF"));
        cursorPaint.setColor(Color.parseColor("#0B57FF"));
        
        cursorPaint.setStrokeWidth(2f);
        gutterDividerLinePaint.setStrokeWidth(1f);
        
        defaultText = getResources().getString(R.string.app_name);        
        whiteSpaceWidth = (int) editableTextPaint.measureText("");

        
        // Set default attributes values
//        setText("UTF-8");
        setTypeface(Typeface.MONOSPACE);
        setTextSize(DisplayUtils.sp2px(context, DEFAULT_TEXT_SIZE_SP));
        setGutterTextAlign(Paint.Align.RIGHT);
        setGutterEnabled(true);
        setDividerLineEnabled(true);
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        requestFocusFromTouch();
        setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    private void setEditable(Editable editable) {
        this.editableText = editable;
        invalidate();
    }
    
    private void setOnEditableListener(EditableListener editableListener) {
        this.editableListener = editableListener;
        invalidate();
    }    

    public void setText(CharSequence text) {
        this.editableText = new Editable(text);
        invalidate();
    }
    
    public void setGutterEnabled(boolean gutterEnabled) {
        this.isGutterEnabled = gutterEnabled;
    }
    
    public void setDividerLineEnabled(boolean dividerLineEnabled) {
        this.isDividerLineEnabled = dividerLineEnabled;
    }
    
    public void setDividerLineWidth(float value) {
        this.gutterDividerLinePaint.setStrokeWidth(value);
    }
    
    public void setIsEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }
    
    public void setIsSelectable(boolean isSelectable) {
        this.isSelectable = isSelectable;
    }
    
    public void setIsCursorDropletVisible(boolean isCursorDropletVisible) {
        this.isCursorDropletVisible = isCursorDropletVisible;
    }

    public void setTextSize(float pixel) {
        float minimum = DisplayUtils.sp2px(getContext(), MINIMUM_TEXT_SIZE_SP);
        float maximum = DisplayUtils.sp2px(getContext(), MAXIMUM_TEXT_SIZE_SP);
        if (pixel < minimum) pixel = minimum;
        if (pixel > maximum) pixel = maximum;
        editableTextPaint.setTextSize(pixel);
        gutterLineNumberPaint.setTextSize(pixel);
        invalidateCursorPosition();
        if(isSelectable) {
        invalidateSelectionRange(selectionStart, selectionEnd);
        }
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        editableTextPaint.setTypeface(typeface);
        gutterLineNumberPaint.setTypeface(typeface);
        invalidate();
    }

    public void setGutterTextAlign(Paint.Align align) {
        this.align = align;
        gutterLineNumberPaint.setTextAlign(align);
        invalidate();
    }
    
    private void setEditableTextAlign(Paint.Align align) {
        this.align = align;
        editableTextPaint.setTextAlign(align);
        invalidate();
    }
    
    public void setCursorWidth(float value) {
        this.cursorPaint.setStrokeWidth(value);
    }    
    
    public Editable getEditable() {
        return this.editableText;
    }
    
    public TextInputConnection getTextInput() {
        return this.textInputConnection;
    }
    
    public GestureEvent getGestureEvent() {
        return this.gestureEvent;  
    } 
    
    public EdgeEffect getVerticalEdgeEffect() {
        return this.edgeEffectVertical;
    }
    
    public EdgeEffect getHorizontalEdgeEffect() {
        return this.edgeEffectHorizontal;
    }
    
    public OverScroller getScroller() {
        return this.scroller;  
    }

    public Paint getEditableBackgroundPaint() {
        return editableBackgroundPaint;
    }

    public Paint getEditableTextPaint() {
        return editableTextPaint;
    }

    public Paint getGutterBackgroundPaint() {
        return gutterBackgroundPaint;
    }

    public Paint getGutterDividerLinePaint() {
        return gutterDividerLinePaint;
    }

    public Paint getGutterLineNumberPaint() {
        return gutterLineNumberPaint;
    }

    public Paint getCurrentLineBackgroundPaint() {
        return currentLineBackgroundPaint;
    }
    
    public Paint getSelectionLineBackgroundPaint() {
        return selectionLineBackgroundPaint;
    } 
    
    public Paint getCursorPaint() {
        return cursorPaint;
    }
    
    public Paint.Align getAlign() {
        return align;
    }
    
    public RectF getVerticalScrollBarRect() {
        return verticalScrollBarRect;
    }

    public RectF getHorizontalScrollBarRect() {
        return horizontalScrollBarRect;
    }

    public void setHorizontalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        horizontalScrollbarThumbDrawable = drawable;
    }

    @Nullable
    public Drawable getHorizontalScrollbarThumbDrawable() {
        return horizontalScrollbarThumbDrawable;
    }

    public void setHorizontalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        horizontalScrollbarTrackDrawable = drawable;
    }

    @Nullable
    public Drawable getHorizontalScrollbarTrackDrawable() {
        return horizontalScrollbarTrackDrawable;
    }

    public void setVerticalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        this.verticalScrollbarThumbDrawable = drawable;
    }

    @Nullable
    public Drawable getVerticalScrollbarThumbDrawable() {
        return verticalScrollbarThumbDrawable;
    }

    public void setVerticalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        verticalScrollbarTrackDrawable = drawable;
    }

    @Nullable
    public Drawable getVerticalScrollbarTrackDrawable() {
        return verticalScrollbarTrackDrawable;
    }    
    
    public Drawable getCursorDropletRes() {
        return cursorDropletRes;
    }
    
    public Drawable getSelectionDropletLeftRes() {
        return selectionDropletLeftRes;
    }
    
    public Drawable getSelectionDropletRightRes() {
        return selectionDropletRightRes;
    }
    
    public int getCursorPosX() {
        return cursorPositionX;
    }
    
    public int getCursorPosY() {
        return cursorPositionY;
    }
    
    public int getCursorWidth() {
        return cursorWidth;
    }
    
    public int getCursorHeight() {
        return cursorHeight;
    }
    
    public int getCursorLine() {
        return cursorLine;
    }
    
    public int getCursorIndex() {
        return cursorIndex;
    }
    
    public int getCursorDropletWidth() {
        return cursorDropletWidth;
    }
    
    public int getCursorDropletHeight() {
        return cursorDropletHeight;
    }
    
    public int getSelectionDropletWidth() {
        return selectionDropletWidth;
    }
    
    public int getSelectionDropletHeight() {
        return selectionDropletHeight;
    }
    
    public int getSelectionDropletLeftX() {
        return selectionDropletLeftX;
    }

    public int getSelectionDropletLeftY() {
        return selectionDropletLeftY;
    }
    
    public int getSelectionDropletRightX() {
        return selectionDropletRightX;
    }
    
    public int getSelectionDropletRightY() {
        return selectionDropletRightY;
    }
    
    public int getScreenWidth() {
        return screenWidth;
    }
    
    public int getScreenHeight() {
        return screenHeight;
    }
    
    public int getWhiteSpaceWidth() {
        return whiteSpaceWidth;
    }
    
    public int getGutterPadding() {
        return GUTTER_PADDING;
    }

    public int getEditablePadding() {
        return EDITABLE_PADDING;
    }
    
    public int getDefaultDuration() {
        return DEFAULT_DURATION;
    }
    
    public int getBlinkTimeout() {
        return BLINK_TIMEOUT;
    }    
    
    public boolean getGutterEnabled() {
        return isGutterEnabled;
    }
    
    public boolean getDividerLineEnabled() {
        return isDividerLineEnabled;
    }
        
    public boolean getIsCursorVisible() {
        return isCursorVisible;
    }
    
    public boolean getIsCursorDropletVisible() {
        return isCursorDropletVisible;
    }
    public boolean getIsEditable() {
        return isEditable;
    }
    
    public boolean IsEditable() {
        return isEditable;
    }        
    
    public boolean getIsSelectable() {
        return isSelectable;
    } 
    
    public boolean getIsReadOnly() {
        return isReadOnly;
    }   

    public int getMeasuredText(String text) {
        return (int) Math.ceil(editableTextPaint.measureText(text));
    }  
    
    public int getGutterWidth() {
        int gutterWidth;
        if (!isGutterEnabled) {
            gutterWidth = 0;
        } else {
            gutterWidth = getMeasuredText(Integer.toString(getLineCount())) + (getGutterPadding() * 2);
        }
        return gutterWidth ;
    }    
    
    public String getLine(int targetLineIndex) {
        return editableText.getLineString(targetLineIndex);
    } 
    
    public String getSelectedText() {
        return editableText.getSelectedText();
    }
    
    public int getLineOffset(int offset) {
        return editableText.getLineOffset(offset);
    } 
    
    public int getLineStart(int targetLineIndex) {
        return editableText.getLineStart(targetLineIndex);
    }
    
    public int getLineEnd(int targetLineIndex) {
        return editableText.getLineEnd(targetLineIndex);
    }
    
    public int getLineLength(int targetLineIndex) {
        return editableText.getLineLength(targetLineIndex);
    }    
    
    public int getLineCount() {
        return editableText.getLineCount();
    }
    
    public int getLineWidth(int targetLineIndex) {
        return editableText.getLineLength(targetLineIndex);
    }
        
    public int getLineHeight() {
        TextPaint.FontMetrics metrics = editableTextPaint.getFontMetrics();
        return (int) Math.ceil(metrics.descent - metrics.ascent);
    } 
    
    public int getSelectionStart() {
        return editableText.getSelectionStart();
    }
    
    public int getSelectionEnd() {
        return editableText.getSelectionEnd();
    }
    
    public int getMaxScrollX() {
        return Math.max(0, (getEditable().length()) * getWidth() - getScreenWidth() / 2);
    }

    public int getMaxScrollY() {
        return Math.max(0, (getLineCount()) * getLineHeight() - getScreenHeight() / 2);
    }
        
    public int getOffsetX() {
        return scroller.getCurrX();
    }
    
    public int getOffsetY() {
        return scroller.getCurrY();
    }
    
    public long getLastTapTime() {
        return lastTapTime;
    }
    
    public Runnable getBlinkAction() {
        return blinkAction;
    }
       
    public ArrayList getReplaceList() {
        return replaceList;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        editableText.setEditableListener(this); 
    }
    
    @Override
    protected void onDetachedFromWindow() {
        editableText.setEditableListener(null);
        super.onDetachedFromWindow();
    }    

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        editorInterface.onDraw(canvas);
    }
    
//    @Override
//    protected void onMeasure(int widthSpec, int heightSpec) {
//        // Handle width measurement
//        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.AT_MOST) {
//            widthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec), MeasureSpec.EXACTLY);
//        }
//
//        // Handle height measurement
//        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.AT_MOST) {
//            int height = getLineHeight() * getLineCount(); // Calculate required height
//            int availableHeight = MeasureSpec.getSize(heightSpec); // Get available height
//
//            // Clamp the height if content exceeds available space
//            if (height > availableHeight) {
//                height = availableHeight;
//            }
//
//            heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
//        }
//
//        super.onMeasure(widthSpec, heightSpec); // Call the superclass implementation
//    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 
                             MeasureSpec.getSize(heightMeasureSpec));
    }    

  /*  @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.AT_MOST) {
//            int availableWidth = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
//            int editableWidth = availableWidth + getEditable().length();
//            
//            if (isWordwrapEnabled || editableWidth > availableWidth) {
//                editableWidth = availableWidth;
//            } 
            widthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.EXACTLY, MeasureSpec.getSize(widthSpec));
        }
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.AT_MOST) {
//            int availableHeight = MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom();
//            int editableHeight = availableHeight + getLineHeight() * getLineCount();
//
//            if (editableHeight > availableHeight) {
//                editableHeight = availableHeight;
//            }
            int height = getLineHeight() * getLineCount();
			//available height
			int aHeight = MeasureSpec.getSize(heightSpec);
			//compare
			if(height > aHeight){
				height = aHeight;
			}
            heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthSpec, heightSpec);
    }*/

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
        
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        invalidate();
    } 
    
    @Override
    public void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        invalidate();
    }    

    @Override
    public void computeScroll() {
        super.computeScroll();
        if(scroller.computeScrollOffset()) {
            invalidate();
        }
    }            
    
    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        invalidate();
    }
    
    @Override
    protected int computeHorizontalScrollRange() {
        return getWidth() * 2;  
    }

    @Override
    protected int computeVerticalScrollRange() {
        return getHeight() * 2;  
    }
    
    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return horizontalScrollBarEnabled;
    }

    @Override
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        this.horizontalScrollBarEnabled = horizontalScrollBarEnabled;
    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return verticalScrollBarEnabled;
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        this.verticalScrollBarEnabled = verticalScrollBarEnabled;
    }
        
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector != null && scaleGestureDetector != null) {
            boolean result = gestureDetector.onTouchEvent(event);
            boolean output = scaleGestureDetector.onTouchEvent(event);
            if (result && output) {
               return true; 
            }
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }    
    
    @Override
    public boolean onCheckIsTextEditor() {
        return super.onCheckIsTextEditor();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
        outAttrs.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE;
        return new TextInputConnection(this);
//        return onCheckIsTextEditor() ? (textInputConnection = new TextInputConnection(this)) : null;
    }

    public void onShowSoftKeyInput() {
        if (!this.hasFocus()) {
            this.requestFocusFromTouch();
        }
        if (inputMethodManager != null) {
            if (isSoftKey && isEditable) {
                inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
            } else if (isHardKey || isReadOnly) {
                inputMethodManager.hideSoftInputFromWindow(this.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }
    }

    public void onInsert(CharSequence editable) {
        if(!isEditable) return;
        if(cursorIndex < 0) return;
   
        cursorIndex = cursorIndex;
        cursorLine = getLineOffset(cursorIndex);            
        
        if(isSelectable) {
            editableText.insert(cursorIndex, editable.toString(), true);
            cursorIndex += selectionStart - selectionEnd;
        } else if (!isSelectable) {
            editableText.insert(cursorIndex, editable.toString(), true);
            cursorIndex += editable.length();
        }
        
        if(editableText.isBatchEdit())
           editableText.endBatchEdit();
        
        if(isSelectable) {
           editableText.beginBatchEdit();
           onDelete();
        }
        
        invalidateCursorPosition();
        invalidate();        
    }

    public void onDelete() {
        if(!isEditable) return; 
        if(cursorIndex <= 0) return;
        
        cursorIndex = cursorIndex;
        cursorLine = getLineOffset(cursorIndex);
        
        if(isSelectable) {
           editableText.delete(selectionStart, selectionEnd, true);
           cursorIndex -= selectionEnd - selectionStart;
        } else if(!isSelectable) {
             editableText.delete(cursorIndex - 1, cursorIndex, true);
             cursorIndex--;
        }
        
        invalidateCursorPosition();
        invalidate();
    }
    
    public void onReplace(CharSequence editable) {
        if(!isEditable) return;
        if(cursorIndex < 0) return;
   
        cursorIndex = cursorIndex;
        cursorLine = getLineOffset(cursorIndex);            
        
        if(isSelectable) {
            editableText.replace(cursorIndex, cursorIndex, editable.toString());
            cursorIndex += selectionStart - selectionEnd;
        } else if (!isSelectable) {
            editableText.replace(cursorLine, cursorIndex, editable.toString());
            cursorIndex += editable.length();
        } 
        
        invalidateCursorPosition();
        invalidate();       
    }
    
    public boolean isUndo() {
        return editableText.isUndo();
    }
    
    public boolean isRedo() {
        return editableText.isRedo();
    }
    
    public void onUndo() {
        int index = editableText.onUndo();
        if(index >= 0) {
            cursorIndex = index;
            cursorLine = getLineOffset(cursorIndex);
            invalidateCursorPosition();
            invalidate();
        }
    }

    public void onRedo() {
        int index = editableText.onRedo();
        if(index >= 0) {
            cursorIndex = index;
            cursorLine = getLineOffset(cursorIndex);
            invalidateCursorPosition();
            invalidate();
        }
    } 
    
    public void onCopy() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.equals("")) {
            ClipData clipData = ClipData.newPlainText("content", selectedText);
            clipboard.setPrimaryClip(clipData);
        }
        isSelectable = false;
    }

    public void onCut() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.equals("")) {
            ClipData clipData = ClipData.newPlainText("content", selectedText);
            clipboard.setPrimaryClip(clipData);
        }
        cursorIndex = cursorIndex;
        cursorLine = getLineOffset(cursorIndex);

        if (selectedText != null && !selectedText.equals("")) {
            editableText.delete(selectionStart, selectionEnd, true);
            cursorIndex -= selectionEnd - selectionStart;
        }
        isSelectable = false;
        invalidateCursorPosition();
    }

    public void onPaste() {
        if (clipboard.hasPrimaryClip()) {
            ClipDescription description = clipboard.getPrimaryClipDescription();
            if (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                ClipData clipData = clipboard.getPrimaryClip();
                ClipData.Item item = clipData.getItemAt(0);
                String copiedText = item.getText().toString();
                onInsert(copiedText);
            }
        }
        isSelectable = false;
        invalidateCursorPosition();
    } 
    
    public void onGoToLine(int targetLineOffsetY) {
        targetLineOffsetY = Math.min(Math.max(targetLineOffsetY, 1), getLineCount());
        cursorIndex = getLineStart(targetLineOffsetY);
        cursorLine = targetLineOffsetY;
        cursorPositionX = getGutterWidth() + getEditablePadding();
        cursorPositionY = (targetLineOffsetY - 1) * getLineHeight();

        // smoothScrollTo(0, Math.max(line * getLineHeight() - getHeight() + getLineHeight() * 2, 0));
    }    
    
    @Override
    public void onInserted(int offset, CharSequence text) {
        
    }

    @Override
    public void onDeleted(int start, int end) {

    }

    @Override
    public void onReplaced(int start, int end, CharSequence text) {

    }

    @Override
    public void onAppended(CharSequence text) {

    }

    public void run() {
        isCursorVisible = !isCursorVisible;
        postDelayed(blinkAction, BLINK_TIMEOUT);

        if (System.currentTimeMillis() - lastTapTime >= 5 * BLINK_TIMEOUT) {
            isCursorDropletVisible = true;
        }
        invalidate();
    }

    public void invalidateCursorPosition() {
        cursorIndex = cursorIndex;
        cursorLine = getLineOffset(cursorIndex);
        cursorPositionX = getGutterWidth() + getMeasuredText(editableText.substring(getLineStart(cursorLine), cursorIndex));
        cursorPositionY = (cursorLine - 1) * getLineHeight();
    }
    
    public void invalidateSelectionRange(int startIndex, int endIndex) {
//        int startLine = editableText.getTargetLineIndex(startIndex);
//        int endLine = getLineOffset(endIndex);
        
        int startLine = getLineStart(editableText.getLineOffset(startIndex));        
        int endLine = getLineStart(getLineOffset(endIndex));        

        selectionDropletLeftX = getGutterWidth() + getMeasuredText(editableText.substring(startLine, startIndex));
        selectionDropletLeftY = startLine * getLineHeight();

        selectionDropletRightX = getGutterWidth() + getMeasuredText(editableText.substring(endLine, endIndex));
        selectionDropletRightY = endLine * getLineHeight();

        selectionStart = startIndex;
        selectionEnd = endIndex;
    }        

    private void setCursorPositionByIndex(int cursorIndex) {
        cursorIndex = cursorIndex;
        cursorLine = getLineOffset(cursorIndex);
        cursorPositionX = getGutterWidth() + getMeasuredText(editableText.substring(getLineStart(cursorLine), cursorIndex));
        cursorPositionY = (cursorLine - 1) * getLineHeight();
    }

    // set cursor position by coordinate
    public void setCursorPositionByCoordinate(float coordinateX, float coordinateY) {
        // calculation the cursor y coordinate
        cursorPositionY = (int) coordinateY / getLineHeight() * getLineHeight();
        int bottom = getLineCount() * getLineHeight();

        if(cursorPositionY < getPaddingTop())
            cursorPositionY = getPaddingTop();

        if(cursorPositionY > bottom - getLineHeight())
            cursorPositionY = bottom - getLineHeight();

        // estimate the cursor x position
        int left = getGutterWidth();

        int prev = left;
        int next = left;

        cursorLine = cursorPositionY / getLineHeight() + 1;
        cursorIndex = getLineStart(cursorLine);

        String text = getLine(cursorLine);
        int length = text.length();

        float[] widths = new float[length];
        editableTextPaint.getTextWidths(text, widths);

        for(int i=0; next < coordinateX && i < length; ++i) {
            if(i > 0) {
                prev += widths[i - 1];
            }
            next += widths[i];
        }

        // calculation the cursor x coordinate
        if(Math.abs(coordinateX - prev) <= Math.abs(next - coordinateX)) {
            cursorPositionX = prev;
        } else {
            cursorPositionX = next;
        }

        // calculation the cursor index
        if(cursorPositionX > left) {
            for(int j=0; left < cursorPositionX && j < length; ++j) {
                left += widths[j];
                ++cursorIndex;
            }
        }
    }    
    
    public void onMove(int slopX, int slopY) {
        int distanceX = 0;
        if(cursorPositionX - getScrollX() <= slopX) {
            distanceX = -getMeasuredText(String.valueOf(editableText.charAt(cursorIndex)));
        } else if(cursorPositionX - getScrollX() >= screenWidth - slopX) {
            distanceX = getMeasuredText(String.valueOf(editableText.charAt(cursorIndex + 1)));
        }   

        if(getHeight() > screenHeight / 2)
            slopY = slopY * 3;

        int distanceY = 0;
        if(cursorPositionY - getScrollY() <= 0) {
            distanceY = -getLineHeight();
        } else if(cursorPositionY - getScrollY() >= getHeight() - slopY) {
            distanceY = getLineHeight();
        }

        if(cursorPositionY + distanceY < 0)
            scrollTo(getScrollX(), 0);
        else if(cursorPositionY + distanceX < 0)
            scrollTo(0, getScrollY());
        else
            scrollBy(distanceX, distanceY);
    }  
    
    // when insert or delete text scroll to visable
    private void scrollToVisable() {
        // horizontal direction
        int distanceX = 0;
        if(cursorPositionX - getScrollX() <= whiteSpaceWidth * 3) 
            distanceX = cursorPositionX - getScrollX() - whiteSpaceWidth * 3;  
        else if(cursorPositionX - getScrollX() >= whiteSpaceWidth - whiteSpaceWidth * 2) 
            distanceX = cursorPositionX - getScrollX() - whiteSpaceWidth + whiteSpaceWidth * 2;

        // vertical direction
        int distanceY = 0;
        if(cursorPositionY - getScrollY() <= 0)
            distanceY = cursorPositionY - getScrollY();
        else if(cursorPositionY - getScrollY() >= getHeight() - getLineHeight())
            distanceY = cursorPositionY - getScrollY() - getHeight() + getLineHeight();

        smoothScrollBy(distanceX, distanceY);
    } 
    
    public final void smoothScrollBy(int distanceX, int distanceY) {
        if(getHeight() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - lastScroll;
        if(duration > DEFAULT_DURATION) {
            scroller.startScroll(getScrollX(), getScrollY(), distanceX, distanceY);
            postInvalidateOnAnimation();
        } else {
            if(!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            scrollBy(distanceX, distanceY);
        }
        lastScroll = AnimationUtils.currentAnimationTimeMillis();
    }


    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }       
}