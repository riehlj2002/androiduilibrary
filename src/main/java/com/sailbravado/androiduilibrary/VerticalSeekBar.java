package com.sailbravado.androiduilibrary;

// TODO: diagnose why the thumb doesn't go to the right position in setProgress().  seems to happen when we setBackground()
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.SeekBar;

/**
 * Implements a vertically-oriented SeekBar.  Some of this code is courtesy of StackOverflow user
 * Fatal1ty2787 (answer posted at <a
 * href="http://stackoverflow.com/questions/4892179/how-can-i-get-a-working-vertical-seekbar-in-android">
 * this</a> link).
 * Created by John Riehl on 3/28/2015.
 */
public class VerticalSeekBar extends SeekBar implements ViewTreeObserver.OnGlobalLayoutListener {
    /**
     * Listener for changes to the VerticalSeekBar.  We include this because it's private in the
     * superclass, not because we're adding any functionality
     */
    @Nullable
    protected OnSeekBarChangeListener mOnSeekBarChangeListener;

    /**
     * Simple constructor to use when creating a VerticalSeekBar from code.
     * @see android.view.View#View(android.content.Context)
     */
    public VerticalSeekBar(Context context) {
        super(context);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * Constructor that is called when inflating a view from XML.
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet, int)
     */
    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource.
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet, int, int)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        // it's annoying that we have to keep track of this in two places...why did the superclass
        // not make this protected vs. private?
        super.setOnSeekBarChangeListener(l);
        mOnSeekBarChangeListener = l;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // simply swap the width and height values
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // simply swap the width and height values
        //noinspection SuspiciousNameCombination
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int progress = getMax() - (int) (getMax() * event.getY() / getHeight());

        if (progress < 0) {
            progress = 0;
        } else if (progress > getMax()) {
            progress = getMax();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setProgress(progress);

                // claim motion events for the VerticalSeekBar until the user releases the drag
                setPressed(true);
                ViewParent parent = getParent();

                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }

                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStartTrackingTouch(this);
                    mOnSeekBarChangeListener.onProgressChanged(this, progress, true);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                int oldProgress = getProgress();
                setProgress(progress);

                // only invoke the callback if the progress has changed
                if ((mOnSeekBarChangeListener != null) && (progress != oldProgress)) {
                    mOnSeekBarChangeListener.onProgressChanged(this, progress, true);
                }

                break;

            case MotionEvent.ACTION_UP:
                oldProgress = getProgress();
                setProgress(progress);
                setPressed(false);

                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStopTrackingTouch(this);

                    if (oldProgress != progress) {
                        mOnSeekBarChangeListener.onProgressChanged(this, progress, true);
                    }
                }

                break;

            case MotionEvent.ACTION_CANCEL:
                setPressed(false);

                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStopTrackingTouch(this);
                }

                break;

            default:
                break;
        }

        return true;
    }

    /**
     * Sets progress for the VerticalSeekBar.
     */
    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);

        // this calls the private superclass method updateThumbAndTrackPos() using the flipped
        // dimensions to accurately set the position of the thumb
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        // set rotation and translation values so that the super class drawing code orients the
        // bar and slider appropriately
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    @Override
    public void onGlobalLayout() {
        setProgress(getProgress());
    }

//    @Override
//    public void setBackgroundColor(int color) {
//        super.setBackgroundColor(color);
//    }
//
//    @Override
//    public void setBackgroundResource(int resid) {
//        super.setBackgroundResource(resid);
//    }
//
//    @Override
//    public void setBackground(Drawable background) {
//        super.setBackground(background);
//    }
//
//    @Override
//    public void setBackgroundDrawable(Drawable background) {
//        super.setBackgroundDrawable(background);
//    }
}