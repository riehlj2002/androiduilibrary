/**
 * 
 */
package com.sailbravado.androiduilibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * A {@link android.preference.DialogPreference} that provides a
 * {@link android.widget.NumberPicker}-based dialog for the user to select an integer value.
 * This <code>Preference</code> will store an <code>int</code> into the
 * <code>SharedPreferences</code>.
 * <h1>Custom XML attributes:</h1>
 * <ul>
 *     <li>
 *         <b>minValue</b> - The minimum value of the NumberPicker
 *     </li>
 *     <li>
 *         <b>maxValue</b> - The maximum value of the NumberPicker
 *     </li>
 * </ul>
 * <h1>Notes for inherited XML attributes:</h1>
 * <ul>
 *      <li>
 *          <b>summary</b> - If specified, a string suitable for
 *          {@link String#format(String, Object...)} that will use the value of the NumberPicker to
 *          display a string
 *      </li>
 *      <li>
 *          <b>dialogLayout</b> - Ignored
 *      </li>
 * </ul>
 * @author John Riehl
 */
public class NumberPickerPreference extends DialogPreference {
	// constructors

	/**
	 * Create a new NumberPickerPreference using a given Context, AttributeSet, and style
	 * @param context The parent Context for the preference.
	 * @param attrs The attributes for the preference.
	 * @param defStyle The style to use for the preference.
	 */
	public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setDialogLayoutResource(R.layout.number_picker_preference_dialog);

		// implement attributes
		TypedArray attributes = context.obtainStyledAttributes(attrs,
                R.styleable.NumberPickerPreference, defStyle, 0);

		if (attributes.hasValue(R.styleable.NumberPickerPreference_minValue)) {
			minValueSpecified = true;
			minValue = attributes.getInteger(R.styleable.NumberPickerPreference_minValue, 0);
		}

		if (attributes.hasValue(R.styleable.NumberPickerPreference_maxValue)) {
			maxValueSpecified = true;
			maxValue = attributes.getInteger(R.styleable.NumberPickerPreference_maxValue, 0);
		}

		attributes.recycle();
	}

	/**
	 * Create a new NumberPickerPreference using a given Context and AttributeSet, using the
     * standard style for a DialogPreference
	 * @param context The parent Context for the preference.
	 * @param attrs The attributes for the preference.
	 */
	public NumberPickerPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}

	// class variables

	/**
	 * Reference to the NumberPicker widget
	 */
	private NumberPicker numberPicker;
	/**
	 * If true, a min value for the NumberPicker was specified in the xml file
	 */
	private boolean minValueSpecified = false;
	/**
	 * The min value for the NumberPicker
	 */
	private int minValue = 0;
	/**
	 * If true, a max value for the NumberPicker was specified in the xml file
	 */
	private boolean maxValueSpecified = false;
	/**
	 * The max value for the NumberPicker
	 */
	private int maxValue = 1;
	/**
	 * The current value
	 */
	private int value;

	// constants

	// lifecycle calls

	/* (non-Javadoc)
	 * @see android.preference.Preference#onGetDefaultValue(android.content.res.TypedArray, int)
	 */
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, 0);
	}

	/* (non-Javadoc)
	 * @see android.preference.Preference#onSetInitialValue(boolean, java.lang.Object)
	 */
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			value = getPersistedInt(0);
		} else {
			value = (Integer) defaultValue;
			persistInt(value);
		}
	}

	/* (non-Javadoc)
	 * @see android.preference.DialogPreference#onCreateDialogView()
	 */
	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		TextView messageTextView =
                (TextView) view.findViewById(R.id.numberPickerPreferenceDialogMessageTextView);
		numberPicker =
                (NumberPicker) view.findViewById(R.id.numberPickerPreferenceDialogNumberPicker);

		if (minValueSpecified) {
			numberPicker.setMinValue(minValue);
		}

		if (maxValueSpecified) {
			numberPicker.setMaxValue(maxValue);
		}

		CharSequence dialogMessage = getDialogMessage();

		if (dialogMessage != null) {
			messageTextView.setVisibility(View.VISIBLE);
			messageTextView.setText(dialogMessage);
		} else {
			messageTextView.setVisibility(View.GONE);
		}

		numberPicker.setValue(value);
		return view;
	}

	/* (non-Javadoc)
	 * @see android.preference.DialogPreference#onSaveInstanceState()
	 */
	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();

		if (isPersistent()) {
			// The value is persisted to the SharedPreferences, so we don't need to save it
			return superState;
		}

		// Create a saved state with the current value
		SavedState savedState = new SavedState(superState);
		savedState.numberPickerValue = value;
		return savedState;
	}

	/* (non-Javadoc)
	 * @see android.preference.DialogPreference#onRestoreInstanceState(android.os.Parcelable)
	 */
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!state.getClass().equals(SavedState.class)) {
			// the saved state isn't of the type that we want, so just call the superclass
			super.onRestoreInstanceState(state);
		} else {
			// call the superclass' restore method with just the part of the state that belongs to it
			SavedState savedState = (SavedState) state;
			super.onRestoreInstanceState(savedState.getSuperState());

			// now set the NumberPicker widget with the value
			numberPicker.setValue(savedState.numberPickerValue);
		}
	}

	/* (non-Javadoc)
	 * @see android.preference.DialogPreference#onDialogClosed(boolean)
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			value = numberPicker.getValue();
			persistInt(value);
			notifyChanged();
		}
	}

	// public methods

    /**
     * Returns the summary of this NumberPickerPreference. If the summary
     * has a {@linkplain String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place.
     *
     * @return the summary with appropriate string substitution
     */
	@Override
	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		return (summary == null) ? null : String.format(summary.toString(), value);
	}

	// private methods

	// callbacks

	// receivers

	// public interfaces and subclasses

	// private interfaces and subclasses

	/**
	 * Class to hold the state of the NumberPickerPreference.
	 * @author John Riehl
	 */
	private static class SavedState extends BaseSavedState {
		// constructors

		/**
		 * Create a saved state for this NumberPickerPreference from a Parcelable object.
		 * @param superState The object from which to create the saved state.
		 */
		public SavedState(Parcelable superState) {
			super(superState);
		}

		/**
		 * Create a saved state for this NumberPickerPreference from an existing Parcel.
		 * @param source The parcel from which to create the saved state.
		 */
		public SavedState(Parcel source) {
			super(source);

			// Get the current value from the parcel
			numberPickerValue = source.readInt();
		}

		// class variables

		/**
		 * The value of the number picker
		 */
		private int numberPickerValue;

		// public methods

		/* (non-Javadoc)
		 * @see android.view.AbsSavedState#writeToParcel(android.os.Parcel, int)
		 */
		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(numberPickerValue);
		}

		/**
		 * Creates a parcel to save the state of the NumberPickerPreference.
		 */
		@SuppressWarnings("unused")
		public static final Creator<SavedState> CREATOR =
				new Creator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel source) {
				return new SavedState(source);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
	
	// exceptions
}
