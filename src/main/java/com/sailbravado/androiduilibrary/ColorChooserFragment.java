package com.sailbravado.androiduilibrary;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;

/**
 * Fragment to implement a color chooser.  Activities that contain this fragment may implement the
 * {@link ColorChooserFragment.OnFragmentInteractionListener} interface to handle changes to the
 * selected color.  ColorChooserFragment also exposes method ({@link #getColor()} to retrieve the
 * current color.
 *
 * Use the {@link com.sailbravado.androiduilibrary.ColorChooserFragment.Builder} factory method to
 * create an instance of this fragment.
 */
public class ColorChooserFragment extends Fragment {
    /**
     * Used to store the current color in the bundle
     */
    private static final String COLOR_KEY = "color";
    private static final int COLOR_DEFAULT = 0xff808080;
    /**
     * Used to store the background color for the result ImageView and opacity SeekBar
     */
    private static final String BACKGROUND_COLOR_KEY = "background_color";
    private static final int BACKGROUND_COLOR_DEFAULT = Color.WHITE;
    /**
     * For the opacity SeekBar and the result ImageView, the spacing of the checkerboard grid
     */
    private static final float CHECKERBOARD_GRID_SIZE = 15;
    /**
     * For the opacity SeekBar and the result ImageView, the portion of the overall size that goes
     * to the margin around the interior
     */
    private static final double BACKGROUND_PORTION = 0.1;
    private static final float SELECTED_COLOR_CIRCLE_STROKE_WIDTH = 2;
    private static final float SELECTED_COLOR_CIRCLE_RADIUS = 20;
    /**
     * The starting resolution for the gradient Bitmap.  Set this to a power of 2.
     */
    private static final int GRADIENT_BITMAP_STARTING_RESOLUTION = 64;
    /**
     * Message to send when the
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.OpacitySeekBarBackgroundWorker
     * OpacitySeekBarBackgroundWorker} is finished.  The {@link android.os.Message#obj obj} field
     * of the message is set to the background Bitmap for the opacity SeekBar.
     */
    private static final int OPACITY_SEEK_BAR_BACKGROUND_WORKER_DONE = 1;
    /**
     * Message to send when the
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.ResultImageViewBackgroundWorker
     * ResultImageViewBackgroundWorker} is finished.  The {@link android.os.Message#obj obj} field
     * of the message is set to the background Bitmap for the result ImageView.
     */
    private static final int RESULT_IMAGE_VIEW_BACKGROUND_WORKER_DONE = 2;
    /**
     * Message to send when the
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.GradientBitmapBackgroundWorker
     * GradientBitmapBackgroundWorker} has a new version of the background Bitmap complete.  The
     * {@link android.os.Message#obj obj} field of the Message is set to the new background Bitmap
     */
    private static final int GRADIENT_BITMAP_BACKGROUND_WORKER_UPDATE = 3;
    /**
     * Message to send when the
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.GradientBitmapBackgroundWorker
     * GradientBitmapBackgroundWorker} is finished.  The {@link android.os.Message#obj obj} field
     * of the message is set to the background Bitmap for the gradient ImageView.
     */
    private static final int GRADIENT_BITMAP_BACKGROUND_WORKER_DONE = 4;
    /**
     * Message to send when the
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.GradientImageViewBackgroundWorker
     * GradientImageViewBackgroundWorker} is finished.  There is no extra data with this message.
     */
    private static final int GRADIENT_IMAGE_VIEW_BACKGROUND_WORKER_DONE = 5;

    // instance fields
    /**
     * If not null, callback for when the color value changes
     */
    @Nullable
    private OnFragmentInteractionListener mListener = null;
    /**
     * The current selected color in AARRGGBB format
     */
    private int mColorARGB;
    /**
     * The current selected color in HSV format
     */
    private float[] mColorHSV = new float[3];
    /**
     * The background color to use for the result ImageView and opacity SeekBar.  This can be set
     * in the builder...if not set, it will be the default theme background color.
     */
    private int mBackgroundColor;
    @NonNull
    private ImageView mGradientImageView;
    @NonNull
    private SeekBar mHueSeekBar;
    @NonNull
    private SeekBar mOpacitySeekBar;
    @NonNull
    private ImageView mResultImageView;
    @NonNull
    private EditText mAlphaEditText;
    @NonNull
    private EditText mRedEditText;
    @NonNull
    private EditText mGreenEditText;
    @NonNull
    private EditText mBlueEditText;
    /**
     * The checkerboard background for the opacity SeekBar
     */
    @Nullable
    private Bitmap mOpacitySeekBarBackgroundBitmap = null;
    /**
     * The inside rectangle in which to draw the gradient of opacity for the selected color
     */
    @NonNull
    private Rect mOpacitySeekBarDrawingRect;
    /**
     * The checkerboard background for the result ImageView
     */
    @Nullable
    private Bitmap mResultImageViewBackgroundBitmap = null;
    /**
     * The inside rectangle in which to draw the resultant selected color
     */
    @NonNull
    private Rect mResultImageViewDrawingRect;
    /**
     * The Bitmap with saturation and value settings for the current hue
     */
    @Nullable
    private Bitmap mGradientImageViewBackgroundBitmap = null;
    /**
     * When non-null there's a thread working on building a background for the opacity SeekBar
     */
    @Nullable
    private OpacitySeekBarBackgroundWorker mOpacitySeekBarBackgroundWorker = null;
    /**
     * When non-null there's a thread working on building the result ImageView
     */
    @Nullable
    private ResultImageViewBackgroundWorker mResultImageViewBackgroundWorker = null;
    /**
     * When non-null there's a thread working on building the gradient Bitmap
     */
    @Nullable
    private GradientBitmapBackgroundWorker mGradientBitmapBackgroundWorker = null;
    /**
     * When non-null there's a thread working on displaying the gradient ImageView
     */
    @Nullable
    private GradientImageViewBackgroundWorker mGradientImageViewBackgroundWorker = null;
    /**
     * A Handler for background workers to send messages to
     */
    @NonNull
    private final WorkerMessageHandler mHandler = new WorkerMessageHandler(this);
    /**
     * Listener for changes to the hue and opacity SeekBars
     */
    @NonNull
    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            // find the new color
            if (seekBar == mHueSeekBar) {
                // new hue, same saturation, value, and alpha
                mColorHSV[0] = progress;
                mColorARGB = Color.HSVToColor(Color.alpha(mColorARGB), mColorHSV);

                // changing the hue changes both the gradient and the opacity views
                if (mGradientBitmapBackgroundWorker != null) {
                    mGradientBitmapBackgroundWorker.cancel(true);
                }

                // the handler will create the GradientImageViewBackgroundWorker when this worker
                // is finished
                mGradientBitmapBackgroundWorker = new GradientBitmapBackgroundWorker(
                        mGradientImageView.getWidth(), mGradientImageView.getHeight(), mHandler);
                mGradientBitmapBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        mColorHSV[0]);

                if (mOpacitySeekBarBackgroundWorker != null) {
                    mOpacitySeekBarBackgroundWorker.cancel(true);
                }

                mOpacitySeekBarBackgroundWorker = new OpacitySeekBarBackgroundWorker(mOpacitySeekBar,
                        mOpacitySeekBarDrawingRect, mBackgroundColor,
                        mOpacitySeekBarBackgroundBitmap, mHandler);
                mOpacitySeekBarBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        mColorARGB);
            } else {
                // this is the opacity SeekBar...only alpha changes
                mColorARGB = Color.argb(progress, Color.red(mColorARGB), Color.green(mColorARGB),
                        Color.blue(mColorARGB));
            }

            // result ImageView and ARGB EditTexts always change
            if (mResultImageViewBackgroundWorker != null) {
                mResultImageViewBackgroundWorker.cancel(true);
            }

            mResultImageViewBackgroundWorker = new ResultImageViewBackgroundWorker(mResultImageView,
                    mResultImageViewDrawingRect, mBackgroundColor, mResultImageViewBackgroundBitmap,
                    mHandler);
            mResultImageViewBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorARGB);
            setARGB();

            if (mListener != null) {
                mListener.onFragmentInteraction(mColorARGB);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    /**
     * Listener for touches to views in the fragment
     */
    @NonNull
    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // if the user has touched something other than an EditText, hide the text input
            // window
            if ((v != mAlphaEditText) && (v != mRedEditText) && (v != mGreenEditText) &&
                    (v != mBlueEditText)) {
                // hide the soft keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);

                if(imm.isAcceptingText()) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                // reset the value of the currently-focused EditText to the current color value
                // (in effect, touching a non-EditText view is akin to "undo typing")
                setARGB();
            }

            // that's all we need to do unless the user has selected the gradient ImageView
            if (v != mGradientImageView) {
                return false;
            }

            // TODO: work out how to handle ACTION_DOWN, ACTION_MOVE, ACTION_UP/etc and use requestDisallowInterceptTouchEvent

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            // find the new color (same hue and alpha, different saturation and value)
            mColorHSV[1] = event.getX() / v.getWidth();
            mColorHSV[2] = event.getY() / v.getHeight();
            mColorARGB = Color.HSVToColor(Color.alpha(mColorARGB), mColorHSV);

            // all the views except the hue SeekBar update
            if (mGradientImageViewBackgroundWorker != null) {
                mGradientImageViewBackgroundWorker.cancel(true);
            }

            mGradientImageViewBackgroundWorker = new GradientImageViewBackgroundWorker(
                    mGradientImageView, mGradientImageViewBackgroundBitmap, mHandler);
            mGradientImageViewBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorHSV[0], mColorHSV[1], mColorHSV[2]);

            if (mOpacitySeekBarBackgroundWorker != null) {
                mOpacitySeekBarBackgroundWorker.cancel(true);
            }

            mOpacitySeekBarBackgroundWorker = new OpacitySeekBarBackgroundWorker(mOpacitySeekBar,
                    mOpacitySeekBarDrawingRect, mBackgroundColor, mOpacitySeekBarBackgroundBitmap,
                    mHandler);
            mOpacitySeekBarBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorARGB);

            if (mResultImageViewBackgroundWorker != null) {
                mResultImageViewBackgroundWorker.cancel(true);
            }

            mResultImageViewBackgroundWorker = new ResultImageViewBackgroundWorker(mResultImageView,
                    mResultImageViewDrawingRect, mBackgroundColor,
                    mResultImageViewBackgroundBitmap, mHandler);
            mResultImageViewBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorARGB);
            setARGB();

            if (mListener != null) {
                mListener.onFragmentInteraction(mColorARGB);
            }

            return true;
        }
    };
    /**
     * Listens for changes to an ARGB value
     */
    @NonNull
    private final View.OnFocusChangeListener mARGBChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                // only need to worry about this when the focus is leaving
                return;
            }

            // bound the input
            Editable text = ((EditText) v).getText();

            try {
                int value = Integer.parseInt(text.toString());

                if (value < 0) {
                    text.replace(0, text.length(), "0");
                } else if (value > 255) {
                    text.replace(0, text.length(), "255");
                }
            } catch (NumberFormatException e) {
                text.replace(0, text.length(), "0");
            }

            // get the new color
            int newARGB;

            try {
                newARGB = Color.argb(
                        Integer.parseInt(mAlphaEditText.getText().toString()),
                        Integer.parseInt(mRedEditText.getText().toString()),
                        Integer.parseInt(mGreenEditText.getText().toString()),
                        Integer.parseInt(mBlueEditText.getText().toString()));
            } catch (NumberFormatException e) {
                // revert to the old color (we know those values are good)
                setARGB();
                newARGB = mColorARGB;
            }

            if (newARGB == mColorARGB) {
                return;
            }

            mColorARGB = newARGB;
            Color.colorToHSV(mColorARGB, mColorHSV);

            // the opacity SeekBar and result ImageView always update
            if (mOpacitySeekBarBackgroundWorker != null) {
                mOpacitySeekBarBackgroundWorker.cancel(true);
            }

            mOpacitySeekBarBackgroundWorker = new OpacitySeekBarBackgroundWorker(mOpacitySeekBar,
                    mOpacitySeekBarDrawingRect, mBackgroundColor, mOpacitySeekBarBackgroundBitmap,
                    mHandler);
            mOpacitySeekBarBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorARGB);
            mOpacitySeekBar.setProgress(Color.alpha(mColorARGB));

            if (mResultImageViewBackgroundWorker != null) {
                mResultImageViewBackgroundWorker.cancel(true);
            }

            mResultImageViewBackgroundWorker = new ResultImageViewBackgroundWorker(mResultImageView,
                    mResultImageViewDrawingRect, mBackgroundColor, mResultImageViewBackgroundBitmap,
                    mHandler);
            mResultImageViewBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorARGB);

            if (mListener != null) {
                mListener.onFragmentInteraction(mColorARGB);
            }

            // if it was only alpha that updated, the hue and gradient don't change
            if (v == mAlphaEditText) {
                return;
            }

            mHueSeekBar.setProgress((int) mColorHSV[0]);

            if (mGradientBitmapBackgroundWorker != null) {
                mGradientBitmapBackgroundWorker.cancel(true);
            }

            // the handler will create the GradientImageViewBackgroundWorker when this worker
            // is finished
            mGradientBitmapBackgroundWorker = new GradientBitmapBackgroundWorker(
                    mGradientImageView.getWidth(), mGradientImageView.getHeight(), mHandler);
            mGradientBitmapBackgroundWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mColorHSV[0]);
        }
    };

    /**
     * Creates a new ColorChooserFragment instance.  Do not use this constructor--instead, use the
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.Builder}.  Using this
     * constructor will result in a RuntimeException being thrown during onCreate()
     */
    public ColorChooserFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args;

        if (savedInstanceState == null) {
            // get the arguments used by the Builder to create the Fragment
            args = getArguments();

            if (args == null) {
                throw new RuntimeException("ColorChooserFragment: no arguments available; use " +
                        "the static Builder class to create a ColorChooserFragment");
            }
        } else {
            args = savedInstanceState;
        }

        mColorARGB = args.getInt(COLOR_KEY, COLOR_DEFAULT);
        Color.colorToHSV(mColorARGB, mColorHSV);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(COLOR_KEY, mColorARGB);
        outState.putInt(BACKGROUND_COLOR_KEY, mBackgroundColor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment and get references to the views.  we'll finish
        // setting up the views in onActivityCreated() because we need to get the background
        // color
        View view = inflater.inflate(R.layout.color_chooser_fragment, container, false);
        mGradientImageView = (ImageView) view.findViewById(R.id.gradientImageView);
        mGradientImageView.requestFocus();
        mResultImageView = (ImageView) view.findViewById(R.id.resultImageView);
        mHueSeekBar = (SeekBar) view.findViewById(R.id.hueSeekBar);
        mOpacitySeekBar = (SeekBar) view.findViewById(R.id.opacitySeekBar);
        mAlphaEditText = (EditText) view.findViewById(R.id.alphaEditText);
        mRedEditText = (EditText) view.findViewById(R.id.redEditText);
        mGreenEditText = (EditText) view.findViewById(R.id.greenEditText);
        mBlueEditText = (EditText) view.findViewById(R.id.blueEditText);

        // set callbacks for touch events on the labels of the ARGB EditTexts.  this is just
        // to allow the user to get out of "edit" mode.  Since we don't need persistent references
        // to the TextViews we'll take care of this here
        view.findViewById(R.id.alphaLabelTextView).setOnTouchListener(mTouchListener);
        view.findViewById(R.id.redLabelTextView).setOnTouchListener(mTouchListener);
        view.findViewById(R.id.greenLabelTextView).setOnTouchListener(mTouchListener);
        view.findViewById(R.id.blueLabelTextView).setOnTouchListener(mTouchListener);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Bundle args;
        super.onActivityCreated(savedInstanceState);

        // we can't get the theme background color until we know the hosting Activity has been
        // created.  this method will set up all the pieces we need that data to set up
        if (savedInstanceState == null) {
            args = getArguments();

            if (args == null) {
                throw new RuntimeException("ColorChooserFragment: no arguments available; use " +
                        "the static Builder class to create a ColorChooserFragment");
            }
        } else {
            args = savedInstanceState;
        }

        if (args.containsKey(BACKGROUND_COLOR_KEY)) {
            mBackgroundColor = args.getInt(BACKGROUND_COLOR_KEY);
        } else {
            try {
                mBackgroundColor = ColorUtils.themeBackgroundColor(getActivity().getTheme());
            } catch (RuntimeException e) {
                mBackgroundColor = BACKGROUND_COLOR_DEFAULT;
            }
        }

        // set up the hue SeekBar
        mHueSeekBar.setProgress((int) mColorHSV[0]);
        mHueSeekBar.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        new HueSeekBarBackgroundWorker(mHueSeekBar).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mHueSeekBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            //noinspection deprecation
                            mHueSeekBar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });
        mHueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mHueSeekBar.setOnTouchListener(mTouchListener);

        // set up the opacity SeekBar
        mOpacitySeekBar.setProgress(Color.alpha(mColorARGB));
        mOpacitySeekBar.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = mOpacitySeekBar.getWidth();
                        int height = mOpacitySeekBar.getHeight();

                        if (mOpacitySeekBar instanceof VerticalSeekBar) {
                            mOpacitySeekBarDrawingRect = new Rect(
                                    (int) (width * BACKGROUND_PORTION),
                                    mOpacitySeekBar.getThumbOffset(),
                                    (int) (width * (1.0 - BACKGROUND_PORTION)),
                                    height - mOpacitySeekBar.getThumbOffset());
                        } else {
                            mOpacitySeekBarDrawingRect = new Rect(
                                    mOpacitySeekBar.getThumbOffset(),
                                    (int) (height * BACKGROUND_PORTION),
                                    width - mOpacitySeekBar.getThumbOffset(),
                                    (int) (height * (1.0 - BACKGROUND_PORTION)));
                        }

                        mOpacitySeekBarBackgroundWorker = new OpacitySeekBarBackgroundWorker(
                                mOpacitySeekBar, mOpacitySeekBarDrawingRect, mBackgroundColor, null,
                                mHandler);
                        mOpacitySeekBarBackgroundWorker.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR, mColorARGB);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mOpacitySeekBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            //noinspection deprecation
                            mOpacitySeekBar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });

        mOpacitySeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mOpacitySeekBar.setOnTouchListener(mTouchListener);

        // set up the result ImageView
        mResultImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = mResultImageView.getWidth();
                        int height = mResultImageView.getHeight();
                        mResultImageViewDrawingRect = new Rect(
                                (int) (width * BACKGROUND_PORTION),
                                (int) (height * BACKGROUND_PORTION),
                                (int) (width * (1.0 - BACKGROUND_PORTION)),
                                (int) (height * (1.0 - BACKGROUND_PORTION)));

                        mResultImageViewBackgroundWorker = new ResultImageViewBackgroundWorker(
                                mResultImageView, mResultImageViewDrawingRect, mBackgroundColor,
                                null, mHandler);
                        mResultImageViewBackgroundWorker.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR, mColorARGB);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mResultImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            //noinspection deprecation
                            mResultImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });
        mResultImageView.setOnTouchListener(mTouchListener);

        // set up the gradient view. the handler will create the GradientImageViewBackgroundWorker
        // when this worker is finished
        mGradientImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mGradientBitmapBackgroundWorker = new GradientBitmapBackgroundWorker(
                                mGradientImageView.getWidth(), mGradientImageView.getHeight(),
                                mHandler);
                        mGradientBitmapBackgroundWorker.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR, mColorHSV[0]);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mGradientImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            //noinspection deprecation
                            mGradientImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });

        mGradientImageView.setOnTouchListener(mTouchListener);

        // set up the ARGB views
        setARGB();
        mAlphaEditText.setOnFocusChangeListener(mARGBChangeListener);
        mRedEditText.setOnFocusChangeListener(mARGBChangeListener);
        mGreenEditText.setOnFocusChangeListener(mARGBChangeListener);
        mBlueEditText.setOnFocusChangeListener(mARGBChangeListener);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Gets the current color set in this fragment
     * @return the current set color
     */
    public int getColor() {
        return mColorARGB;
    }

    /**
     * sets the EditText fields with the current ARGB values
     */
    private void setARGB() {
        mAlphaEditText.setText(Integer.toString(Color.alpha(mColorARGB)));
        mRedEditText.setText(Integer.toString(Color.red(mColorARGB)));
        mGreenEditText.setText(Integer.toString(Color.green(mColorARGB)));
        mBlueEditText.setText(Integer.toString(Color.blue(mColorARGB)));
    }

    /**
     * sets up the gradient
     */

    /**
     * Activities that implement this interface will receive callbacks when the lists change.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Called when the selected color changes.
         *
         * @param color The newly-selected color in AARRGGBB format.
         */
        public void onFragmentInteraction(int color);
    }

    /**
     * Provides a mechanism to create a ListChooserFragment.
     */
    public static class Builder {
        // Static fields
        // Instance fields

        /**
         * Arguments for the constructor.
         */
        @NonNull
        private final Bundle args;

        // Constructors

        /**
         * Creates a new instance of the ListChooserFragment builder.
         */
        public Builder() {
            args = new Bundle();
        }

        // Static methods
        // Instance methods

        /**
         * Creates a {@link com.sailbravado.androiduilibrary.ColorChooserFragment} with the
         * arguments supplied to this builder.
         * @return The ColorChooserFragment
         */
        @NonNull
        public ColorChooserFragment create() {
            ColorChooserFragment fragment = new ColorChooserFragment();
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * Sets the background color for the opacity SeekBar and result ImageView.  The default is
         * the theme background color, or Color.WHITE if the theme didn't specify a background color
         * @param backgroundColor The background color.
         * @return This {@link com.sailbravado.androiduilibrary.ColorChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        @NonNull
        public Builder setBackgroundColor(int backgroundColor) {
            args.putInt(BACKGROUND_COLOR_KEY, backgroundColor);
            return this;
        }

        /**
         * Sets the color for the ColorChooserFragment.  The default is 0xFF808080 (gray).
         * @param color The color.
         * @return This {@link com.sailbravado.androiduilibrary.ColorChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        @NonNull
        public Builder setColor(int color) {
            args.putInt(COLOR_KEY, color);
            return this;
        }
    }

    /**
     * Builds the background for the hue SeekBar in a worker thread.  Since the background for the
     * hue SeekBar doesn't change, this worker is run just once from onCreateView() and can be
     * declared static (see
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.OpacitySeekBarBackgroundWorker
     * OpacityBackgroundWorker} for the approach we use for a changing background).  The execute()
     * method does not take any parameters.
     */
    private static class HueSeekBarBackgroundWorker extends AsyncTask<Void, Void, Bitmap> {
        @NonNull
        private final WeakReference<SeekBar> mSeekBarReference;
        private final int mWidth;
        private final int mHeight;
        private final int mThumbOffset;
        private final boolean mIsVertical;

        /**
         * Create a worker to build the hue SeekBar background on the given drawable.
         * @param seekBar The hue SeekBar
         */
        public HueSeekBarBackgroundWorker(@NonNull SeekBar seekBar) {
            mSeekBarReference = new WeakReference<>(seekBar);
            mWidth = seekBar.getWidth();
            mHeight = seekBar.getHeight();
            mThumbOffset = seekBar.getThumbOffset();
            mIsVertical = seekBar instanceof VerticalSeekBar;
        }

        @Override
        @Nullable
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setStrokeWidth(0);    // use hairline
            float[] hsv = new float[] {0, 1, 1};

            if (mIsVertical) {
                float hueIncrement = (float) (360.0 / (double) (mHeight - (2 * mThumbOffset)));

                for (float y = mHeight - mThumbOffset; y > mThumbOffset; y -= 1, hsv[0] += hueIncrement) {
                    paint.setColor(Color.HSVToColor(hsv));
                    canvas.drawLine(0, y, mWidth, y, paint);
                }
            } else {
                float hueIncrement = (float) (360.0 / (double) (mWidth - (2 * mThumbOffset)));

                for (float x = mThumbOffset; x < mWidth - mThumbOffset; x += 1, hsv[0] += hueIncrement) {
                    paint.setColor(Color.HSVToColor(hsv));
                    canvas.drawLine(x, 0, x, mHeight, paint);
                }
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                SeekBar seekBar = mSeekBarReference.get();

                if (seekBar != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        seekBar.setBackground(new BitmapDrawable(seekBar.getResources(), bitmap));
                    } else {
                        //noinspection deprecation
                        seekBar.setBackgroundDrawable(new BitmapDrawable(seekBar.getResources(),
                                bitmap));
                    }
                }
            }
        }
    }

    /**
     * Builds the background for the opacity SeekBar in a background thread.  The execute() method
     * takes one parameter--the color to draw.  The worker will send a {@link android.os.Message
     * Message} back through an {@link android.os.Handler Handler} with a reference to the
     * checkerboard background bitmap to worker creates.
     */
    private static class OpacitySeekBarBackgroundWorker extends AsyncTask<Integer, Void, Bitmap> {
        @NonNull
        private final WeakReference<SeekBar> mSeekBarReference;
        @NonNull
        private final WeakReference<Rect> mDrawingRectReference;
        @NonNull
        private final WeakReference<Bitmap> mBackgroundBitmapReference;
        private final int mBackgroundColor;
        @Nullable
        private Bitmap mBackgroundBitmap;
        @NonNull
        private final WeakReference<WorkerMessageHandler> mHandlerReference;
        private final int mWidth;
        private final int mHeight;
        private final int mThumbOffset;
        private final boolean mIsVertical;

        /**
         * Create a worker to build the opacity SeekBar background on the given drawable.
         * @param seekBar The opacity SeekBar
         * @param drawingRect The area in the SeekBar background in which to draw the different
         *                    opacity settings corresponding to the current color
         * @param backgroundColor The color to use as the background for the opacity SeekBar.  This
         *                        value is ignored if backgroundBitmap is specified
         * @param backgroundBitmap A reference to the bitmap with the checkerboard background.  If
         *                         <code>null</code> the worker will create a checkerboard Bitmap
         * @param handler The {@link android.os.Handler Handler} to which send a
         * {@link android.os.Message Message} indicating completion.
         */
        public OpacitySeekBarBackgroundWorker(
                @NonNull SeekBar seekBar,
                @NonNull Rect drawingRect,
                int backgroundColor,
                @Nullable Bitmap backgroundBitmap,
                @NonNull WorkerMessageHandler handler) {
            mSeekBarReference = new WeakReference<>(seekBar);
            mDrawingRectReference = new WeakReference<>(drawingRect);
            mBackgroundBitmapReference = new WeakReference<>(backgroundBitmap);
            mBackgroundColor = backgroundColor;
            mHandlerReference = new WeakReference<>(handler);
            mWidth = seekBar.getWidth();
            mHeight = seekBar.getHeight();
            mThumbOffset = seekBar.getThumbOffset();
            mIsVertical = seekBar instanceof VerticalSeekBar;
        }

        @Override
        @Nullable
        protected Bitmap doInBackground(Integer... params) {
            mBackgroundBitmap = mBackgroundBitmapReference.get();

            if (mBackgroundBitmap == null) {
                mBackgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mBackgroundBitmap);
                Paint paint = new Paint();
                paint.setColor(ColorUtils.isDark(mBackgroundColor) ? Color.WHITE : Color.BLACK);
                paint.setStyle(Paint.Style.FILL);

                if (mIsVertical) {
                    for (float y = mThumbOffset; y < mHeight - mThumbOffset; y += CHECKERBOARD_GRID_SIZE) {
                        if (isCancelled() || (mSeekBarReference.get() == null)) {
                            return null;
                        }

                        for (float x = 0; x < mWidth; x += CHECKERBOARD_GRID_SIZE) {
                            canvas.drawRect(x, y,
                                    x + CHECKERBOARD_GRID_SIZE / 2, y + CHECKERBOARD_GRID_SIZE / 2,
                                    paint);
                        }
                    }
                } else {
                    for (float x = mThumbOffset; x < mWidth - mThumbOffset; x += CHECKERBOARD_GRID_SIZE) {
                        if (isCancelled() || (mSeekBarReference.get() == null)) {
                            return null;
                        }

                        for (float y = 0; y < mHeight; y += CHECKERBOARD_GRID_SIZE) {
                            canvas.drawRect(x, y,
                                    x + CHECKERBOARD_GRID_SIZE / 2, y + CHECKERBOARD_GRID_SIZE / 2,
                                    paint);
                        }
                    }
                }
            }

            // Add the opacity settings for the color to the checkerboard background
            assert mBackgroundBitmap != null;
            // TODO: handle Bitmap.copy failure
            Bitmap bitmap = mBackgroundBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(params[0]);
            paint.setStrokeWidth(0);
            Rect drawingRect = mDrawingRectReference.get();

            if (drawingRect == null) {
                // the fragment was destroyed
                return null;
            }

            float alpha = 0;

            if (mIsVertical) {
                float alphaIncrement = (float) (255.0 / (double) drawingRect.height());

                for (float y = drawingRect.bottom; y > drawingRect.top; y -= 1, alpha += alphaIncrement) {
                    if (isCancelled() || (mSeekBarReference.get() == null)) {
                        return null;
                    }

                    paint.setAlpha((int) alpha);
                    canvas.drawLine(drawingRect.left, y, drawingRect.right, y, paint);
                }
            } else {
                float alphaIncrement = (float) (255.0 / (double) drawingRect.width());

                for (float x = drawingRect.left; x < drawingRect.right; x += 1, alpha += alphaIncrement) {
                    if (isCancelled() || (mSeekBarReference.get() == null)) {
                        return null;
                    }

                    paint.setAlpha((int) alpha);
                    canvas.drawLine(x, drawingRect.top, x, drawingRect.bottom, paint);
                }
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }

            SeekBar seekBar = mSeekBarReference.get();

            if (seekBar != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    seekBar.setBackground(new BitmapDrawable(seekBar.getResources(), bitmap));
                } else {
                    //noinspection deprecation
                    seekBar.setBackgroundDrawable(new BitmapDrawable(seekBar.getResources(),
                            bitmap));
                }
            }

            WorkerMessageHandler handler = mHandlerReference.get();

            if (handler != null) {
                assert mBackgroundBitmap != null;
                handler.sendMessage(handler.obtainMessage(OPACITY_SEEK_BAR_BACKGROUND_WORKER_DONE,
                        mBackgroundBitmap));
            }
        }
    }

    /**
     * Builds the result ImageView in a background thread.  The execute() method takes one
     * parameter--the color to draw.  The worker will send a {@link android.os.Message Message} back
     * through a {@link android.os.Handler Handler} with a reference to the checkerboard background
     * bitmap to worker creates.
     */
    private static class ResultImageViewBackgroundWorker extends AsyncTask<Integer, Void, Bitmap> {
        @NonNull
        private final WeakReference<ImageView> mImageViewReference;
        @NonNull
        private final WeakReference<Rect> mDrawingRectReference;
        @NonNull
        private final WeakReference<Bitmap> mBackgroundBitmapReference;
        private final int mBackgroundColor;
        @Nullable
        private Bitmap mBackgroundBitmap;
        @NonNull
        private final WeakReference<WorkerMessageHandler> mHandlerReference;
        private final int mWidth;
        private final int mHeight;

        /**
         * Create a worker to build the result ImageView.
         * @param imageView The ImageView that holds the result color
         * @param drawingRect The area in the ImageView in which to draw the result color
         * @param backgroundColor The color to use as the background for the result ImageView
         * @param backgroundBitmap A reference to the bitmap with the checkerboard background.  If
         *                         <code>null</code> the worker will create a checkerboard Bitmap
         *                         and store it {@link #mResultImageViewBackgroundBitmap}.
         * @param handler The {@link android.os.Handler Handler} to which to send a
         *                {@link android.os.Message Message} indicating completion.
         */
        public ResultImageViewBackgroundWorker(
                @NonNull ImageView imageView,
                @NonNull Rect drawingRect,
                int backgroundColor,
                @Nullable Bitmap backgroundBitmap,
                @NonNull WorkerMessageHandler handler) {
            mImageViewReference = new WeakReference<>(imageView);
            mDrawingRectReference = new WeakReference<>(drawingRect);
            mBackgroundBitmapReference = new WeakReference<>(backgroundBitmap);
            mBackgroundColor = backgroundColor;
            mHandlerReference = new WeakReference<>(handler);
            mWidth = imageView.getWidth();
            mHeight = imageView.getHeight();
        }

        @Override
        @Nullable
        protected Bitmap doInBackground(Integer... params) {
            mBackgroundBitmap = mBackgroundBitmapReference.get();

            if (mBackgroundBitmap == null) {
                mBackgroundBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mBackgroundBitmap);
                Paint paint = new Paint();
                paint.setColor(mBackgroundColor);
                canvas.drawPaint(paint);
                paint.setColor(ColorUtils.isDark(mBackgroundColor) ? Color.WHITE : Color.BLACK);
                paint.setStyle(Paint.Style.FILL);

                for (float x = 0; x < mWidth; x += CHECKERBOARD_GRID_SIZE) {
                    if (isCancelled() || (mImageViewReference.get() == null)) {
                        return null;
                    }

                    for (float y = 0; y < mHeight; y += CHECKERBOARD_GRID_SIZE) {
                        canvas.drawRect(x, y,
                                x + CHECKERBOARD_GRID_SIZE / 2, y + CHECKERBOARD_GRID_SIZE / 2,
                                paint);
                    }
                }
            }

            assert mBackgroundBitmap != null;
            // TODO: handle Bitmap.copy failure
            Bitmap bitmap = mBackgroundBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(params[0]);
            Rect drawingRect = mDrawingRectReference.get();

            if (drawingRect == null) {
                // the fragment was destroyed
                return null;
            }

            canvas.drawRect(drawingRect, paint);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }

            ImageView imageView = mImageViewReference.get();

            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }

            WorkerMessageHandler handler = mHandlerReference.get();

            if (handler != null) {
                assert mBackgroundBitmap != null;
                handler.sendMessage(handler.obtainMessage(RESULT_IMAGE_VIEW_BACKGROUND_WORKER_DONE,
                        mBackgroundBitmap));
            }
        }
    }

    /**
     * Builds the Bitmap for the gradient ImageView in a background thread.  The execute() method
     * takes one parameter--the hue to draw.  The worker will send a
     * {@link android.os.Message Message} back through a {@link android.os.Handler Handler} with a
     * reference to the gradient Bitmap the worker creates.  Note that this worker does not put the
     * circle on the ImageView indicating the saturation and value of the given hue...that's done in
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.GradientImageViewBackgroundWorker
     * GradientImageViewBackgroundWorker}.
     */
    private static class GradientBitmapBackgroundWorker extends AsyncTask<Float, Bitmap, Bitmap> {
        @NonNull
        private final WeakReference<WorkerMessageHandler> mHandlerReference;
        private final int mWidth;
        private final int mHeight;

        /**
         * Creates a new worker to build a Bitmap of the given size.
         * @param width Width of the gradient Bitmap
         * @param height Height of the gradient Bitmap
         * @param handler If not null, Handler to which to send a message when complete
         */
        public GradientBitmapBackgroundWorker(int width, int height,
                                              @NonNull WorkerMessageHandler handler) {
            mHandlerReference = new WeakReference<>(handler);
            mWidth = width;
            mHeight = height;
        }

        @Override
        @Nullable
        protected Bitmap doInBackground(Float... params) {
            // start off with low-resolution versions to speed up progress
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);

            for (int res = GRADIENT_BITMAP_STARTING_RESOLUTION; res > 1; res /= 2) {
                float[] hsv = new float[] {params[0], 0, 0};
                Bitmap gradientBitmap = Bitmap.createBitmap(mWidth, mHeight,
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(gradientBitmap);
                float saturationIncrement = (float) ((double) res / (double) mWidth);
                float valueIncrement = (float) ((double) res / (double) mWidth);

                for (int x = 0; x < mWidth; x+= res, hsv[1] += saturationIncrement) {
                    if (isCancelled()) {
                        return null;
                    }

                    hsv[2] = 0;

                    for (int y = 0; y < mHeight; y += res, hsv[2] += valueIncrement) {
                        paint.setColor(Color.HSVToColor(0xff, hsv));
                        canvas.drawRect(x, y, x + res, y + res, paint);
                    }
                }

                publishProgress(gradientBitmap);
            }

            // now do the full resolution version
            float[] hsv = new float[] {params[0], 0, 0};
            Bitmap gradientBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            float saturationIncrement = (float) (1.0 / (double) mWidth);
            float valueIncrement = (float) (1.0 / (double) mHeight);

            for (int x = 0; x < mWidth; x++, hsv[1] += saturationIncrement) {
                if (isCancelled()) {
                    return null;
                }

                hsv[2] = 0;

                for (int y = 0; y < mHeight; y++, hsv[2] += valueIncrement) {
                    gradientBitmap.setPixel(x, y, Color.HSVToColor(hsv));
                }
            }

            return gradientBitmap;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            WorkerMessageHandler handler = mHandlerReference.get();

            if (handler != null) {
                handler.sendMessage(handler.obtainMessage(GRADIENT_BITMAP_BACKGROUND_WORKER_UPDATE,
                        values[0]));
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            WorkerMessageHandler handler = mHandlerReference.get();

            if ((bitmap != null) && (handler != null)) {
                handler.sendMessage(handler.obtainMessage(GRADIENT_BITMAP_BACKGROUND_WORKER_DONE,
                        bitmap));
            }
        }
    }

    /**
     * Puts the current saturation and value on a gradient Bitmap created by
     * {@link com.sailbravado.androiduilibrary.ColorChooserFragment.GradientBitmapBackgroundWorker
     * GradientBitmapBackgrundWorker} and posts the resulting Bitmap on the gradient ImageView.  The
     * execute() method takes three parameters--the hue, saturation and value to draw.  The worker
     * will send a {@link android.os.Message Message} back through a
     * {@link android.os.Handler Handler} when finished.
     */
    private static class GradientImageViewBackgroundWorker extends AsyncTask<Float, Void, Bitmap> {
        @NonNull
        private final WeakReference<ImageView> mImageViewReference;
        @NonNull
        private final WeakReference<Bitmap> mGradientBitmapReference;
        @NonNull
        private final WeakReference<WorkerMessageHandler> mHandlerReference;
        private final int mWidth;
        private final int mHeight;

        /**
         * Create a new background worker to put the saturation and value indicator on the given
         * gradient Bitmap and post it to the given ImageView
         * @param imageView The ImageView that holds the result color
         * @param gradientBitmap The gradient Bitmap on which to draw the indicator.  If this is
         *                       null the worker does nothing
         * @param handler The {@link android.os.Handler Handler} to which to send a
         * {@link android.os.Message Message} indicating completion.
         */
        public GradientImageViewBackgroundWorker(@NonNull ImageView imageView,
                                                 @Nullable Bitmap gradientBitmap,
                                                 @NonNull WorkerMessageHandler handler) {
            mImageViewReference = new WeakReference<>(imageView);
            mWidth = imageView.getWidth();
            mHeight = imageView.getHeight();
            mGradientBitmapReference = new WeakReference<>(gradientBitmap);
            mHandlerReference = new WeakReference<>(handler);
        }

        @Override
        protected Bitmap doInBackground(Float... params) {
            Bitmap gradientBitmap = mGradientBitmapReference.get();

            if (gradientBitmap == null) {
                return null;
            }

            Bitmap bitmap = gradientBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setStrokeWidth(SELECTED_COLOR_CIRCLE_STROKE_WIDTH);
            paint.setStyle(Paint.Style.STROKE);
            float[] hsv = new float[] {params[0], params[1], params[2]};
            paint.setColor(ColorUtils.isDark(Color.HSVToColor(hsv)) ? Color.WHITE : Color.BLACK);
            canvas.drawCircle(params[1] * mWidth, params[2] * mHeight, SELECTED_COLOR_CIRCLE_RADIUS,
                    paint);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }

            ImageView imageView = mImageViewReference.get();

            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }

            WorkerMessageHandler handler = mHandlerReference.get();

            if (handler != null) {
                handler.sendMessage(handler.obtainMessage(
                        GRADIENT_IMAGE_VIEW_BACKGROUND_WORKER_DONE));
            }
        }
    }

    /**
     * A Handler to receive and process completion messages from the various background workers.
     */
    private static class WorkerMessageHandler extends Handler {
        private final WeakReference<ColorChooserFragment> mFragmentReference;

        /**
         * Create the Handler and associate it with the given Fragment.
         * @param fragment The Fragment
         */
        WorkerMessageHandler(ColorChooserFragment fragment) {
            super();
            mFragmentReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            ColorChooserFragment fragment = mFragmentReference.get();

            if (fragment == null) {
                // the fragment has been destroyed
                return;
            }

            switch (msg.what) {
                case OPACITY_SEEK_BAR_BACKGROUND_WORKER_DONE:
                    fragment.mOpacitySeekBarBackgroundBitmap = (Bitmap) msg.obj;
                    fragment.mOpacitySeekBarBackgroundWorker = null;
                    break;

                case RESULT_IMAGE_VIEW_BACKGROUND_WORKER_DONE:
                    fragment.mResultImageViewBackgroundBitmap = (Bitmap) msg.obj;
                    fragment.mResultImageViewBackgroundWorker = null;
                    break;

                case GRADIENT_BITMAP_BACKGROUND_WORKER_UPDATE:
                case GRADIENT_BITMAP_BACKGROUND_WORKER_DONE:
                    // the worker thread has a Bitmap for us to use.  if this is an _UPDATE message
                    // then the Bitmap is an interim and the worker thread will continue
                    if (msg.what == GRADIENT_BITMAP_BACKGROUND_WORKER_DONE) {
                        fragment.mGradientBitmapBackgroundWorker = null;
                    }

                    fragment.mGradientImageViewBackgroundBitmap = (Bitmap) msg.obj;

                    if (fragment.mGradientImageViewBackgroundWorker != null) {
                        fragment.mGradientImageViewBackgroundWorker.cancel(true);
                    }

                    fragment.mGradientImageViewBackgroundWorker =
                            new GradientImageViewBackgroundWorker(fragment.mGradientImageView,
                                    fragment.mGradientImageViewBackgroundBitmap, this);
                    fragment.mGradientImageViewBackgroundWorker.executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR, fragment.mColorHSV[0],
                            fragment.mColorHSV[1], fragment.mColorHSV[2]);
                    break;

                case GRADIENT_IMAGE_VIEW_BACKGROUND_WORKER_DONE:
                    fragment.mGradientImageViewBackgroundWorker = null;
                    break;
            }
        }
    }
}
