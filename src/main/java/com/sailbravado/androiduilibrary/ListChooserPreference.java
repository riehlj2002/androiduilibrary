package com.sailbravado.androiduilibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;

// TODO: put version control in such that a change to the logic that generates the shared preference is reflected all the way through
// TODO: figure out how summary will work (I can't figure out how it worked for the NumberPickerPreference)
/**
 * A {@link android.preference.DialogPreference} that provides a
 * {@link com.sailbravado.androiduilibrary.ListChooserFragment}-based dialog for the user to
 * select items from a source list to send to a destination list.  This <code>Preference</code>
 * will store a delimited <code>String</code> in the <code>SharedPreference</code>.  Use the static
 * methods {@link #getSourceEntries} and {@link #getDestinationEntries} to parse the value retrieved
 * from {@link android.content.SharedPreferences#getString(String, String)} into <code>String</code>
 * arrays.  Use the static method {@link #createSharedPreferenceValue} to create the value stored in
 * the <code>SharedPreference</code> from source and destination arrays.
 * <h1>Custom XML attributes:</h1>
 * <ul>
 *     <li>
 *         <b>sourceListLabel</b> - A String with the label to put at the top of the source entry
 *         list.  If not specified, there will be no label.
 *     </li>
 *     <li>
 *         <b>defaultSourceEntries</b> - A StringArray with the source entries.  The strings must
 *         not contain the delimiter string...the constructor will throw a
 *         <code>IllegalArgumentException</code> if they do.  This attribute is not mandatory...if
 *         not specified, the default will be an empty list.
 *     </li>
 *     <li>
 *         <b>sourceListIsSortable</b> - If true, the user can sort the source list (default is
 *         false).
 *     </li>
 *     <li>
 *         <b>autoSortSourceList</b> - If true, the source list automatically sorts (default is
 *         false).
 *     </li>
 *     <li>
 *         <b>destinationListLabel</b> - A String with the label to put at the top of the
 *         destination entry list.  If not specified, there will be no label.
 *     </li>
 *     <li>
 *         <b>destinationList</b> - Similar to sourceList, except it applies to the destination
 *         entries.
 *     </li>
 *     <li>
 *         <b>destinationListIsSortable</b> - If true, the user can sort the destination list
 *         (default is false).
 *     </li>
 *     <li>
 *         <b>autoSortDestinationList</b> - If true, the destination list automatically sorts
 *         (default is false).
 *     </li>
 *     <li>
 *         <b>delimiter</b> - The delimiter string for the stored <code>SharedPreference</code>.
 *         Te default value is {@link #DEFAULT_LIST_CHOOSER_PREFERENCE_DELIMITER}.
 *     </li>
 * </ul>
 * <h1>Notes for inherited XML attributes:</h1>
 * <ul>
 *      <li>
 *          <b>summary</b> - If specified, a string suitable for
 *          {@link String#format(String, Object...)} that will use the value of the
 *          ListChooserFragment to display a string
 *      </li>
 *      <li>
 *          <b>dialogLayout</b> - Ignored
 *      </li>
 *      <li>
 *          <b>defaultValue</b> - Ignored
 *      </li>
 * </ul>
 * Created by John on 2/16/2015.
 */
public class ListChooserPreference extends DialogPreference {

    /*
    Notes:
        This differs from the standard DialogPreference pattern in a few respects.

        - Since it uses the ListChooserFragment as its UI, I don't implement onSaveInstanceState and
        onRestoreInstanceState, since all the instance data is managed internal to the fragment.

        - Since the data I'm storing as a preference is two lists (i.e. not one of the standard
        types for a SharedPreference), onSetInitialValue will look a bit different than normal.
     */

    // Static fields
    /**
     * The default delimiter for storing the lists in this preference.
     */
    public static final String DEFAULT_LIST_CHOOSER_PREFERENCE_DELIMITER = "\\";

    /**
     * The label to show at the top of the source list.
     */
    private final String mSourceListLabel;

    /**
     * The default source entries.
     */
    private String[] mDefaultSourceEntries;

    /**
     * If true, the source list is sortable.
     */
    private final boolean mSourceListIsSortable;

    /**
     * If true, the source list automatically sorts.
     */
    private final boolean mAutoSortSourceList;

    /**
     * The label to show at the top of the destination lists.
     */
    private final String mDestinationListLabel;

    /**
     * The default destination entries.
     */
    private String[] mDefaultDestinationEntries;

    /**
     * If true, the destination list is sortable.
     */
    private final boolean mDestinationListIsSortable;

    /**
     * If true, the destination list automatically sorts.
     */
    private final boolean mAutoSortDestinationList;

    // Instance fields
    /**
     * Delimiter to use when storing or retrieving entries.
     */
    @NonNull
    private String mDelimiter = DEFAULT_LIST_CHOOSER_PREFERENCE_DELIMITER;

    /**
     * The fragment for setting the lists.
     */
    private ListChooserFragment mListChooserFragment;

    // Constructors

    /**
     * Perform inflation from XML and apply a class-specific base style.
     * @param context The Context this is associated with, through which it can access the current
     *                theme, resources, SharedPreferences, etc.
     * @param attrs The attributes of the XML tag that is inflating the preference.
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default values for the view. Can be 0 to not look
     *                     for defaults.
     * @throws java.lang.IllegalArgumentException if the delimiter is contained in any of the
     * default source or destination entry lists
     */
    public ListChooserPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.list_chooser_preference_dialog);

        // implement attributes
        TypedArray attributes = context.obtainStyledAttributes(attrs,
                R.styleable.ListChooserPreference, defStyleAttr, 0);

        mSourceListLabel =
                attributes.getString(R.styleable.ListChooserPreference_sourceListLabel);

        // the getTextArray() method returns null if the attribute isn't specified
        mDefaultSourceEntries = (String[])
                attributes.getTextArray(R.styleable.ListChooserPreference_defaultSourceEntries);

        if (mDefaultSourceEntries == null) {
            mDefaultSourceEntries = new String[0];
        }

        mSourceListIsSortable =
                attributes.getBoolean(R.styleable.ListChooserPreference_sourceListIsSortable,
                        false);
        mAutoSortSourceList =
                attributes.getBoolean(R.styleable.ListChooserPreference_autoSortSourceList, false);

        mDestinationListLabel =
                attributes.getString(R.styleable.ListChooserPreference_destinationListLabel);

        mDefaultDestinationEntries = (String[])
                attributes.getTextArray(R.styleable.ListChooserPreference_defaultDestinationEntries);

        if (mDefaultDestinationEntries == null) {
            mDefaultDestinationEntries = new String[0];
        }

        mDestinationListIsSortable =
                attributes.getBoolean(R.styleable.ListChooserPreference_destinationListIsSortable,
                        false);
        mAutoSortDestinationList =
                attributes.getBoolean(R.styleable.ListChooserPreference_autoSortDestinationList,
                        false);

        if (attributes.hasValue(R.styleable.ListChooserPreference_delimiter)) {
            mDelimiter = attributes.getString(R.styleable.ListChooserPreference_delimiter);
        }

        attributes.recycle();

        // verify the delimiter is not contained in the lists
        for (String entry : mDefaultSourceEntries) {
            if (entry.contains(mDelimiter)) {
                throw new IllegalArgumentException("delimiter " +  mDelimiter +
                        " found in default source entry " + entry);
            }
        }

        for (String entry : mDefaultDestinationEntries) {
            if (entry.contains(mDelimiter)) {
                throw new IllegalArgumentException("delimiter " + mDelimiter +
                        " found in default destination entry " + entry);
            }
        }
    }

    /**
     * Constructor that is called when inflating a Preference from XML. This uses the style
     * associated with PreferenceDialogs.
     * @see #ListChooserPreference(android.content.Context, android.util.AttributeSet, int)
     */
    public ListChooserPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    // Static methods

    /**
     * Builds an array of source entries from the given preference value using the given delimiter.
     * If there are no source entries in the preference value, this method returns an empty array.
     * @param preferenceValue The preference value, as retrieved from
     * {@link android.content.SharedPreferences#getString(String, String)}.
     * @param delimiter A delimiter to parse the string.
     * @return An array of source entries.
     */
    public static ArrayList<String> getSourceEntries(@Nullable String preferenceValue,
                                            @NonNull String delimiter) {
        if ((preferenceValue == null) || (preferenceValue.length() == 0)) {
            return new ArrayList<>();
        }

        // splitting the lists is a doubling of the delimiter.  if there's no double delimiter,
        // there are just source entries.
        String[] lists = preferenceValue.split("\\Q" + delimiter + delimiter + "\\E");
        ArrayList<String> entries = new ArrayList<>();

        // single delimiter splits the entries in the list
        Collections.addAll(entries, lists[0].split("\\Q" + delimiter + "\\E"));
        return entries;
    }

    /**
     * Builds an array of destination entries from the given preference value using the given
     * delimiter.  If there are no destination entries in the preference value, this method returns
     * an empty array.
     * @param preferenceValue The preference value, as retrieved fro
     * {@link android.content.SharedPreferences#getString(String, String)}.
     * @param delimiter A delimiter to parse the string.
     * @return An array of destination entries.
     */
    public static ArrayList<String> getDestinationEntries(@Nullable String preferenceValue,
                                                 @NonNull String delimiter) {
        if ((preferenceValue == null) || (preferenceValue.length() == 0)) {
            return new ArrayList<>();
        }

        // splitting the lists is a doubling of the delimiter.  if there's no double delimiter,
        // there are just source entries.
        String[] lists = preferenceValue.split("\\Q" + delimiter + delimiter + "\\E");

        if (lists.length == 1) {
            return new ArrayList<>();
        }

        ArrayList<String> entries = new ArrayList<>();

        // single delimiter splits the entries in the list
        Collections.addAll(entries, lists[1].split("\\Q" + delimiter + "\\E"));
        return entries;
    }

    /**
     * Builds a preference value from the given arrays (either of which can be <code>null</code>)
     * and the given delimiter (which must not be <code>null</code>).
     * @param sourceEntries The source entries.
     * @param destinationEntries The destination entries.
     * @param delimiter The delimiter to use.
     * @return The preference value.
     */
    public static String createSharedPreferenceValue(@Nullable ArrayList<String> sourceEntries,
                                                     @Nullable ArrayList<String> destinationEntries,
                                                     @NonNull String delimiter) {
        String preferenceValue = "";

        if (sourceEntries != null) {
            int i = 0;
            int count = sourceEntries.size();

            for (; i < count - 1; i++) {
                preferenceValue += sourceEntries.get(i) + delimiter;
            }

            preferenceValue += sourceEntries.get(i);
        }

        preferenceValue += delimiter + delimiter;

        if (destinationEntries != null) {
            int i = 0;
            int count = destinationEntries.size();

            for (; i < count - 1; i++) {
                preferenceValue += destinationEntries.get(i) + delimiter;
            }

            preferenceValue += destinationEntries.get(i);
        }

        return preferenceValue;
    }

    // Instance methods
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistString(createSharedPreferenceValue(mListChooserFragment.getSourceEntries(),
                    mListChooserFragment.getDestinationEntries(), mDelimiter));
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        ArrayList<String> sourceEntries;
        ArrayList<String> destinationEntries;

        if (restorePersistedValue) {
            String sharedPreferenceValue = getPersistedString("");
            sourceEntries = getSourceEntries(sharedPreferenceValue, mDelimiter);
            destinationEntries = getDestinationEntries(sharedPreferenceValue, mDelimiter);
        } else {
            sourceEntries = new ArrayList<>();
            Collections.addAll(sourceEntries, mDefaultSourceEntries);
            destinationEntries = new ArrayList<>();
            Collections.addAll(destinationEntries, mDefaultDestinationEntries);
            persistString(createSharedPreferenceValue(sourceEntries, destinationEntries,
                    mDelimiter));
        }

        mListChooserFragment = new ListChooserFragment.Builder()
                .setSourceListLabel(mSourceListLabel)
                .setSourceList(sourceEntries)
                .setSourceListIsSortable(mSourceListIsSortable)
                .setAutoSortSourceList(mAutoSortSourceList)
                .setDestinationListLabel(mDestinationListLabel)
                .setDestinationList(destinationEntries)
                .setDestinationListIsSortable(mDestinationListIsSortable)
                .setAutoSortDestinationList(mAutoSortDestinationList)
                .create();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected View onCreateDialogView() {
        View view = super.onCreateDialogView();
        getDialog().getOwnerActivity().getFragmentManager().beginTransaction()
                .add(R.id.listChooserFrameLayout, mListChooserFragment)
                .commit();
        return view;
    }

    // Static inner classes

    // Inner classes
}
