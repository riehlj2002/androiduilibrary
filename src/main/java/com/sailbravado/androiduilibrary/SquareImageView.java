package com.sailbravado.androiduilibrary;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link android.widget.ImageView ImageView} that can size itself to be square.  Thanks to
 * Andro Selva, see
 * <a href="http://stackoverflow.com/questions/16506275/imageview-be-a-square-with-dynamic-width">
 * this</a> for more information.  Use the <code>dependentDimension</code> attribute with a value
 * of <code>horizontal</code>, <code>vertical</code>, or <code>neither</code> to control which
 * dimension will size itself to match the other.  That dimension will ignore the corresponding
 * <code>layout_</code> attribute.  The default is neither (which is in essence a regular
 * <code>ImageView</code>).
 * <p><p>Created by John Riehl on 4/19/2015.
 */
public class SquareImageView extends ImageView {
    /**
     * The dimension that resizes to match its counterpart.
     */
    protected DependentDimension mDependentDimension = DependentDimension.NEITHER;

    /**
     * Simple constructor to use when creating a SquareImageView from code.
     * @see android.view.View#View(android.content.Context)
     */
    public SquareImageView(Context context) {
        super(context);
    }

    /**
     * Constructor called when creating a SquareImageView from XML.
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public SquareImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet, int)
     */
    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getCustomAttributes(context, attrs);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute or
     * style resource.
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet, int, int)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        getCustomAttributes(context, attrs);
    }

    /**
     * Extract the custom attributes from the XML.
     */
    protected void getCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.SquareImageView, 0, 0);

        try {
            mDependentDimension = DependentDimension.fromInt(
                    a.getInt(R.styleable.SquareImageView_dependentDimension,
                            mDependentDimension.toInt()));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // on the measure pass, set measured dimensions according to the setting of the dependent
        // dimension
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        switch (mDependentDimension) {
            case HORIZONTAL:
                //noinspection SuspiciousNameCombination
                setMeasuredDimension(height, height);
                break;

            case VERTICAL:
                //noinspection SuspiciousNameCombination
                setMeasuredDimension(width, width);
                break;

            case NEITHER:
                setMeasuredDimension(width, height);
                break;
        }
    }

    /**
     * Return the value of the <code>dependentDimension</code> attribute.
     */
    public DependentDimension getDependentDimension() {
        return mDependentDimension;
    }

    /**
     * Sets a new value for the <code>dependentDimension</code> attribute and recomputes the layout
     * based on that value.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setDependentDimension(final DependentDimension dependentDimension) {
        if (mDependentDimension == dependentDimension) {
            return;
        }

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) && isInLayout()) {
            // wait until the current layout pass is finished before requesting a new one
            getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mDependentDimension = dependentDimension;
                            requestLayout();
                        }
                    });
        } else {
            mDependentDimension = dependentDimension;
            requestLayout();
        }
    }

    /**
     * Valid values for the dependentDimension attribute.
     */
    public enum DependentDimension {
        // important: don't change these without also changing the corresponding values in attrs.xml
        /**
         * The horizontal dimension of the {@link com.sailbravado.androiduilibrary.SquareImageView
         * SquareImageView} will resize to match the vertical dimension upon layout.
         */
        HORIZONTAL(0),
        /**
         * The vertical dimension of the {@link com.sailbravado.androiduilibrary.SquareImageView
         * SquareImageView} will resize to match the horizontal dimension upon layout.
         */
        VERTICAL(1),
        /**
         * Neither dimension of the {@link com.sailbravado.androiduilibrary.SquareImageView
         * SquareImageView} will resize to match the other dimension upon layout.  This is in
         * essence equivalent to a {@link android.widget.ImageView ImageView}.
         */
        NEITHER(2);

        /**
         * Stores a map of valid int values for the enums
         */
        private static final Map<Integer, DependentDimension> TYPES_BY_VALUE = new HashMap<>();

        static {
            for (DependentDimension type : DependentDimension.values()) {
                TYPES_BY_VALUE.put(type.mValue, type);
            }
        }

        /**
         * Stores the current value of the DependentDimension enum
         */
        private int mValue;

        /**
         * Private constructor to initialize the value.
         * @param value The value to initialize.
         */
        private DependentDimension(int value) {
            mValue = value;
        }

        /**
         * Returns the value as an int.
         */
        public int toInt() {
            return mValue;
        }

        /**
         * Returns a DependentDimension that corresponds to the given int value.
         * @throws java.lang.RuntimeException RuntimeException if the int doesn't map to a valid
         * DependentDimension value.
         */
        public static DependentDimension fromInt(int value) {
            DependentDimension dependentDimension = TYPES_BY_VALUE.get(value);

            if (dependentDimension == null) {
                throw new RuntimeException(DependentDimension.class.getName() + ".fromInt(): value "
                        + value + " is not valid for this enum");
            }

            return dependentDimension;
        }
    }
}
