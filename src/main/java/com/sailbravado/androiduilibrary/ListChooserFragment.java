package com.sailbravado.androiduilibrary;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

// TODO: make the lists generic (relies on Object.toString() since a comparator can't be stored in a Bundle)
// TODO: allow for setting of styles
// TODO: base the color of the button symbology on the actual button backgrounds (this will allow for arbitrarily-styled buttons)
/**
 * Fragment to implement a list chooser.  The fragment displays two lists, and the user can move
 * entries between the lists.  The frament provides controls for the user to manually sort the lists.
 * <p></p>Activities that contain this fragment may implement the
 * {@link ListChooserFragment.OnFragmentInteractionListener} interface to handle changes to the
 * lists.  ListChooserFragment also exposes methods ({@link #getSourceEntries()} and
 * {@link #getDestinationEntries()}) to retrieve the current source and destination
 * lists.
 *
 * Use the {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} factory method to
 * create an instance of this fragment.
 */
public class ListChooserFragment extends Fragment {
    // the fragment initialization parameters
    private static final String SOURCE_LIST_KEY = "source_list";
    private static final String SOURCE_LIST_LABEL_KEY = "source_list_label";
    private static final String SOURCE_LIST_IS_SORTABLE_KEY = "source_list_is_sortable";
    private static final boolean SOURCE_LIST_IS_SORTABLE_DEFAULT = false;
    private static final String AUTO_SORT_SOURCE_LIST_KEY = "auto_sort_source_list";
    private static final boolean AUTO_SORT_SOURCE_LIST_DEFAULT = false;
    private static final String DESTINATION_LIST_KEY = "destination_list";
    private static final String DESTINATION_LIST_LABEL_KEY = "destination_list_label";
    private static final String DESTINATION_LIST_IS_SORTABLE_KEY = "destination_list_is_sortable";
    private static final boolean DESTINATION_LIST_IS_SORTABLE_DEFAULT = false;
    private static final String AUTO_SORT_DESTINATION_LIST_KEY = "auto_sort_destination";
    private static final boolean AUTO_SORT_DESTINATION_LIST_DEFAULT = false;

    /**
     * Alpha value to apply to enabled buttons.
     */
    private int enabledButtonAlpha;

    /**
     * Alpha value to apply to disabled buttons.
     */
    private int disabledButtonAlpha;

    /**
     * The text to display above the source list.  If <code>null</code> don't display a label.
     */
    @Nullable
    private String mSourceListLabel;

    /**
     * If <code>true</code> allow the user to sort the source entries.
     */
    private boolean mSourceListIsSortable;

    /**
     * If <code>true</code> automatically sort the source entries.
     */
    private boolean mAutoSortSourceList;

    /**
     * The text to display above the destination list.  If <code>null</code> don't display a label.
     */
    @Nullable
    private String mDestinationListLabel;

    /**
     * If <code>true</code> allow the user to sort the destination entries.
     */
    private boolean mDestinationListIsSortable;

    /**
     * If <code>true</code> automatically sort the destination entries.
     */
    private boolean mAutoSortDestinationList;

    /**
     * Callback when there's interaction with the fragment.
     */
    @Nullable
    private OnFragmentInteractionListener mListener = null;

    /**
     * Reference to the button that adds selected entries to the destination list
     */
    @NonNull
    private ImageButton mAddEntryImageButton;

    /**
     * Reference to the button that adds all entries to the destination list
     */
    @NonNull
    private ImageButton mAddAllEntriesImageButton;

    /**
     * Reference to the button that removes selected entries from the destination list
     */
    @NonNull
    private ImageButton mRemoveEntryImageButton;

    /**
     * Reference to the button that removes all entries from the destination list
     */
    @NonNull
    private ImageButton mRemoveAllEntriesImageButton;

    /**
     * Reference to the button that moves a source entry up in the source list
     */
    @NonNull
    private ImageButton mMoveSourceEntryUpImageButton;

    /**
     * Reference to the button that moves a source entry down in the source list
     */
    @NonNull
    private ImageButton mMoveSourceEntryDownImageButton;

    /**
     * Reference to the button that moves a destination entry up in the destination list
     */
    @NonNull
    private ImageButton mMoveDestinationEntryUpImageButton;

    /**
     * Reference to the button that moves a destination entry down in the destination list
     */
    @NonNull
    private ImageButton mMoveDestinationEntryDownImageButton;

    /**
     * Reference to the source ListView
     */
    @NonNull
    private ListView mSourceListView;

    /**
     * Reference to the destination ListView
     */
    @NonNull
    private ListView mDestinationListView;

    /**
     * ArrayAdapter for the source list
     */
    @NonNull
    private ArrayAdapter<String> mSourceArrayAdapter;

    /**
     * ArrayAdapter for the destination list
     */
    @NonNull
    private ArrayAdapter<String> mDestinationArrayAdapter;

    /**
     * Listener for when an item in the source list is clicked.
     */
    private final AdapterView.OnItemClickListener mSourceItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    setSourceButtonState();
                }
            };

    /**
     * Listener for when an item in the destination list is clicked.
     */
    private final AdapterView.OnItemClickListener mDestinationItemClickListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    setDestinationButtonState();
                }
            };

    /**
     * Listener for presses to the buttons that sort the destination list
     */
    private final ImageButton.OnClickListener mSortButtonListener =
            new ImageButton.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean moveUp = true;
                    ListView listView;
                    ArrayAdapter<String> arrayAdapter;

                    if (v == mMoveSourceEntryUpImageButton) {
                        listView = mSourceListView;
                        arrayAdapter = mSourceArrayAdapter;
                    } else if (v == mMoveSourceEntryDownImageButton) {
                        listView = mSourceListView;
                        arrayAdapter = mSourceArrayAdapter;
                        moveUp = false;
                    } else if (v == mMoveDestinationEntryUpImageButton) {
                        listView = mDestinationListView;
                        arrayAdapter = mDestinationArrayAdapter;
                    } else {
                        listView = mDestinationListView;
                        arrayAdapter = mDestinationArrayAdapter;
                        moveUp = false;
                    }

                    SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                    int count = arrayAdapter.getCount();

                    if (moveUp) {
                        // the code disables the move up button when the first item in the
                        // list is checked
                        for (int i = 1; i < count; i++) {
                            if (checkedItems.get(i)) {
                                String item = arrayAdapter.getItem(i);
                                arrayAdapter.remove(item);
                                arrayAdapter.insert(item, i - 1);
                            }

                            listView.setItemChecked(i - 1, checkedItems.get(i));
                        }

                        listView.setItemChecked(count - 1, false);
                    } else {
                        // the code disables the move down button when the last item in the
                        // list is checked
                        for (int i = count - 2; i >= 0; i--) {
                            if (checkedItems.get(i)) {
                                String item = arrayAdapter.getItem(i);
                                arrayAdapter.remove(item);
                                arrayAdapter.insert(item, i + 1);
                            }

                            listView.setItemChecked(i + 1, checkedItems.get(i));
                        }

                        listView.setItemChecked(0, false);
                    }

                    setSourceButtonState();
                    setDestinationButtonState();

                    if (mListener != null) {
                        mListener.onFragmentInteraction(getSourceEntries(), getDestinationEntries());
                    }
                }
    };

    /**
     * Listener for presses to the buttons that add to the destination list
     */
    private final ImageButton.OnClickListener mAddButtonListener =
            new ImageButton.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean addAll = v == mAddAllEntriesImageButton;
                    SparseBooleanArray checkedItems = mSourceListView.getCheckedItemPositions();
                    ArrayList<String> items = new ArrayList<>();
                    int count = mSourceArrayAdapter.getCount();

                    for (int i = 0; i < count; i++) {
                        if (addAll || checkedItems.get(i)) {
                            items.add(mSourceArrayAdapter.getItem(i));
                        }
                    }

                    for (String item : items) {
                        mSourceArrayAdapter.remove(item);
                        mDestinationArrayAdapter.add(item);
                    }

                    if (mAutoSortDestinationList) {
                        mDestinationArrayAdapter.sort(String.CASE_INSENSITIVE_ORDER);
                    }

                    clearCheckedItems();
                    setSourceButtonState();
                    setDestinationButtonState();

                    if (mListener != null) {
                        mListener.onFragmentInteraction(getSourceEntries(), getDestinationEntries());
                    }
                }
    };

    /**
     * Listener for presses to the buttons that remove from the destination list
     */
    private final ImageButton.OnClickListener mRemoveButtonListener =
            new ImageButton.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean removeAll = v == mRemoveAllEntriesImageButton;
                    SparseBooleanArray checkedItems = mDestinationListView.getCheckedItemPositions();
                    ArrayList<String> items = new ArrayList<>();
                    int count = mDestinationArrayAdapter.getCount();

                    for (int i = 0; i < count; i++) {
                        if (removeAll || checkedItems.get(i)) {
                            items.add(mDestinationArrayAdapter.getItem(i));
                        }
                    }

                    for (String item : items) {
                        mDestinationArrayAdapter.remove(item);
                        mSourceArrayAdapter.add(item);
                    }

                    if (mAutoSortSourceList) {
                        mSourceArrayAdapter.sort(String.CASE_INSENSITIVE_ORDER);
                    }

                    clearCheckedItems();
                    setSourceButtonState();
                    setDestinationButtonState();

                    if (mListener != null) {
                        mListener.onFragmentInteraction(getSourceEntries(), getDestinationEntries());
                    }
                }
    };

    /**
     * Creates a new ListChooserFragment instance.  Do not use this constructor--instead, use the
     * {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder}.  Using this constructor
     * will result in a RuntimeException being thrown during onCreate()
     */
    public ListChooserFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the arguments used to create the Fragment
        Bundle args = getArguments();

        if (args == null) {
            throw new RuntimeException("ListChooserFragment: no arguments available; use the " +
                    "static Builder class to create a ListChooserFragment");
        }

        mSourceListLabel = args.getString(SOURCE_LIST_LABEL_KEY);
        mAutoSortSourceList = args.getBoolean(AUTO_SORT_SOURCE_LIST_KEY,
                AUTO_SORT_SOURCE_LIST_DEFAULT);
        mSourceListIsSortable = !mAutoSortSourceList &&
                args.getBoolean(SOURCE_LIST_IS_SORTABLE_KEY, SOURCE_LIST_IS_SORTABLE_DEFAULT);
        mDestinationListLabel = args.getString(DESTINATION_LIST_LABEL_KEY);
        mAutoSortDestinationList = args.getBoolean(AUTO_SORT_DESTINATION_LIST_KEY,
                AUTO_SORT_DESTINATION_LIST_DEFAULT);
        mDestinationListIsSortable = !mAutoSortDestinationList &&
                args.getBoolean(DESTINATION_LIST_IS_SORTABLE_KEY, DESTINATION_LIST_IS_SORTABLE_DEFAULT);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SOURCE_LIST_KEY, getSourceEntries());
        outState.putStringArrayList(DESTINATION_LIST_KEY, getDestinationEntries());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.list_chooser_fragment, container, false);

        // set the labels for the lists
        TextView textView = (TextView) view.findViewById(R.id.sourceListLabelTextView);

        if (mSourceListLabel == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(mSourceListLabel);
        }

        textView = (TextView) view.findViewById(R.id.destinationListLabelTextView);

        if (mDestinationListLabel == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(mDestinationListLabel);
        }

        // get the background color of the current theme amd the current screen orientation in order
        // to determine what to put on the buttons
        boolean backgroundIsDark = ColorUtils.isDark(
                ColorUtils.themeBackgroundColor(getActivity().getTheme()));

        // alpha values for enabled and disabled buttons (see http://developer.android.com/design/style/iconography.html)
        enabledButtonAlpha = backgroundIsDark ? 204 : 153; // 80% for dark backgrounds, 60% for light
        disabledButtonAlpha = 77; // 30%
        boolean screenIsLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        // set up the sorting buttons
        mMoveSourceEntryDownImageButton =
                (ImageButton) view.findViewById(R.id.moveSourceEntryDownImageButton);
        mMoveSourceEntryUpImageButton =
                (ImageButton) view.findViewById(R.id.moveSourceEntryUpImageButton);
        mMoveDestinationEntryDownImageButton =
                (ImageButton) view.findViewById(R.id.moveDestinationEntryDownImageButton);
        mMoveDestinationEntryUpImageButton =
                (ImageButton) view.findViewById(R.id.moveDestinationEntryUpImageButton);

        if (mSourceListIsSortable) {
            mMoveSourceEntryUpImageButton.setOnClickListener(mSortButtonListener);
            mMoveSourceEntryUpImageButton.setImageResource(backgroundIsDark ?
                    R.drawable.ic_action_single_up_arrow_dark_background :
                    R.drawable.ic_action_single_up_arrow_light_background);
            mMoveSourceEntryDownImageButton.setOnClickListener(mSortButtonListener);
            mMoveSourceEntryDownImageButton.setImageResource(backgroundIsDark ?
                    R.drawable.ic_action_single_down_arrow_dark_background :
                    R.drawable.ic_action_single_down_arrow_light_background);
        } else {
            mMoveSourceEntryUpImageButton.setVisibility(View.GONE);
            mMoveSourceEntryDownImageButton.setVisibility(View.GONE);
        }

        if (mDestinationListIsSortable) {
            mMoveDestinationEntryUpImageButton.setOnClickListener(mSortButtonListener);
            mMoveDestinationEntryUpImageButton.setImageResource(backgroundIsDark ?
                    R.drawable.ic_action_single_up_arrow_dark_background :
                    R.drawable.ic_action_single_up_arrow_light_background);
            mMoveDestinationEntryDownImageButton.setOnClickListener(mSortButtonListener);
            mMoveDestinationEntryDownImageButton.setImageResource(backgroundIsDark ?
                    R.drawable.ic_action_single_down_arrow_dark_background :
                    R.drawable.ic_action_single_down_arrow_light_background);
        } else {
            mMoveDestinationEntryUpImageButton.setVisibility(View.GONE);
            mMoveDestinationEntryDownImageButton.setVisibility(View.GONE);
        }

        // set up the adapters for the two lists.  if this is the first invocation, use the lists
        // specified in the arguments when created
        Bundle listSource = (savedInstanceState == null) ? getArguments() : savedInstanceState;
        mSourceArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.array_adapter_text_view,
                listSource.getStringArrayList(SOURCE_LIST_KEY));
        mDestinationArrayAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.array_adapter_text_view,
                listSource.getStringArrayList(DESTINATION_LIST_KEY));

        // now set up the lists themselves
        mSourceListView = (ListView) view.findViewById(R.id.sourceListView);
        mSourceListView.setAdapter(mSourceArrayAdapter);
        mSourceListView.setOnItemClickListener(mSourceItemClickListener);
        mDestinationListView = (ListView) view.findViewById(R.id.destinationListView);
        mDestinationListView.setAdapter(mDestinationArrayAdapter);
        mDestinationListView.setOnItemClickListener(mDestinationItemClickListener);

        // now set up the buttons
        mAddEntryImageButton = (ImageButton) view.findViewById(R.id.addEntryImageButton);
        mAddEntryImageButton.setOnClickListener(mAddButtonListener);
        mAddEntryImageButton.setImageResource(backgroundIsDark ?
                screenIsLandscape ? R.drawable.ic_action_single_right_arrow_dark_background :
                                    R.drawable.ic_action_single_down_arrow_dark_background :
                screenIsLandscape ? R.drawable.ic_action_single_right_arrow_light_background :
                                    R.drawable.ic_action_single_down_arrow_light_background);
        mAddAllEntriesImageButton = (ImageButton) view.findViewById(R.id.addAllEntriesImageButton);
        mAddAllEntriesImageButton.setOnClickListener(mAddButtonListener);
        mAddAllEntriesImageButton.setImageResource(backgroundIsDark ?
                screenIsLandscape ? R.drawable.ic_action_double_right_arrow_dark_background :
                                    R.drawable.ic_action_double_down_arrow_dark_background :
                screenIsLandscape ? R.drawable.ic_action_double_right_arrow_light_background :
                                    R.drawable.ic_action_double_down_arrow_light_background);
        mRemoveEntryImageButton = (ImageButton) view.findViewById(R.id.removeEntryImageButton);
        mRemoveEntryImageButton.setOnClickListener(mRemoveButtonListener);
        mRemoveEntryImageButton.setImageResource(backgroundIsDark ?
                screenIsLandscape ? R.drawable.ic_action_single_left_arrow_dark_background :
                                    R.drawable.ic_action_single_up_arrow_dark_background :
                screenIsLandscape ? R.drawable.ic_action_single_left_arrow_light_background :
                                    R.drawable.ic_action_single_up_arrow_light_background);
        mRemoveAllEntriesImageButton =
                (ImageButton) view.findViewById(R.id.removeAllEntriesImageButton);
        mRemoveAllEntriesImageButton.setOnClickListener(mRemoveButtonListener);
        mRemoveAllEntriesImageButton.setImageResource(backgroundIsDark ?
                screenIsLandscape ? R.drawable.ic_action_double_left_arrow_dark_background :
                                    R.drawable.ic_action_double_up_arrow_dark_background :
                screenIsLandscape ? R.drawable.ic_action_double_left_arrow_light_background :
                                    R.drawable.ic_action_double_up_arrow_light_background);

        setSourceButtonState();
        setDestinationButtonState();

        return view;
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
     * Gets the current source entries.  The array returned is a copy of the source list, not a
     * reference to the list itself.
     * @return the source entries
     */
    @NonNull
    public ArrayList<String> getSourceEntries() {
        int count = mSourceArrayAdapter.getCount();
        ArrayList<String> entries = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            entries.add(mSourceArrayAdapter.getItem(i));
        }

        return entries;
    }

    /**
     * Gets the current destination entries.  The array returned is a copy of the destination
     * list, not a reference to the list itself.
     * @return the destination entries
     */
    @NonNull
    public ArrayList<String> getDestinationEntries() {
        int count = mDestinationArrayAdapter.getCount();
        ArrayList<String> entries = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            entries.add(mDestinationArrayAdapter.getItem(i));
        }

        return entries;
    }

    /**
     * Sets the states of the buttons according to the state of the source list.
     */
    private void setSourceButtonState() {
        boolean itemsPresent = mSourceListView.getCount() > 0;
        boolean itemsChecked = itemsPresent && (mSourceListView.getCheckedItemCount() > 0);
        mAddEntryImageButton.setEnabled(itemsChecked);
        mAddEntryImageButton.getDrawable().setAlpha(itemsChecked ? enabledButtonAlpha : disabledButtonAlpha);
        mAddAllEntriesImageButton.setEnabled(itemsPresent);
        mAddAllEntriesImageButton.getDrawable()
                .setAlpha(itemsPresent ? enabledButtonAlpha : disabledButtonAlpha);

        if (mSourceListIsSortable) {
            // can't move things up if the first item is checked, and can't move them down if the
            // last item is checked
            if (itemsChecked) {
                SparseBooleanArray checkedItems = mSourceListView.getCheckedItemPositions();
                boolean firstItemChecked = checkedItems.get(0);
                boolean lastItemChecked = checkedItems.get(mSourceListView.getCount() - 1);
                mMoveSourceEntryUpImageButton.setEnabled(!firstItemChecked);
                mMoveSourceEntryUpImageButton.getDrawable()
                        .setAlpha(firstItemChecked ? disabledButtonAlpha : enabledButtonAlpha);
                mMoveSourceEntryDownImageButton.setEnabled(!lastItemChecked);
                mMoveSourceEntryDownImageButton.getDrawable()
                        .setAlpha(lastItemChecked ? disabledButtonAlpha : enabledButtonAlpha);
            } else {
                mMoveSourceEntryDownImageButton.setEnabled(false);
                mMoveSourceEntryDownImageButton.getDrawable().setAlpha(disabledButtonAlpha);
                mMoveSourceEntryUpImageButton.setEnabled(false);
                mMoveSourceEntryUpImageButton.getDrawable().setAlpha(disabledButtonAlpha);
            }
        }
    }

    /**
     * Sets the states of the buttons according to the state of the destination list.
     */
    private void setDestinationButtonState() {
        boolean itemsPresent = mDestinationListView.getCount() > 0;
        boolean itemsChecked = itemsPresent && (mDestinationListView.getCheckedItemCount() > 0);
        mRemoveEntryImageButton.setEnabled(itemsChecked);
        mRemoveEntryImageButton.getDrawable()
                .setAlpha(itemsChecked ? enabledButtonAlpha : disabledButtonAlpha);
        mRemoveAllEntriesImageButton.setEnabled(itemsPresent);
        mRemoveAllEntriesImageButton.getDrawable()
                .setAlpha(itemsPresent ? enabledButtonAlpha : disabledButtonAlpha);

        if (mDestinationListIsSortable) {
            if (itemsChecked) {
                // can't move things up if the first item is checked, and can't move them down if the
                // last item is checked
                SparseBooleanArray checkedItems = mDestinationListView.getCheckedItemPositions();
                boolean firstItemChecked = checkedItems.get(0);
                boolean lastItemChecked = checkedItems.get(mDestinationListView.getCount() - 1);
                mMoveDestinationEntryUpImageButton.setEnabled(!firstItemChecked);
                mMoveDestinationEntryUpImageButton.getDrawable()
                        .setAlpha(firstItemChecked ? disabledButtonAlpha : enabledButtonAlpha);
                mMoveDestinationEntryDownImageButton.setEnabled(!lastItemChecked);
                mMoveDestinationEntryDownImageButton.getDrawable()
                        .setAlpha(lastItemChecked ? disabledButtonAlpha : enabledButtonAlpha);
            } else {
                mMoveDestinationEntryDownImageButton.setEnabled(false);
                mMoveDestinationEntryDownImageButton.getDrawable().setAlpha(disabledButtonAlpha);
                mMoveDestinationEntryUpImageButton.setEnabled(false);
                mMoveDestinationEntryUpImageButton.getDrawable().setAlpha(disabledButtonAlpha);
            }
        }
    }

    private void clearCheckedItems() {
        int count = mSourceListView.getCount();

        for (int i = 0; i < count; i++) {
            mSourceListView.setItemChecked(i, false);
        }

        count = mDestinationListView.getCount();

        for (int i = 0; i < count; i++) {
            mDestinationListView.setItemChecked(i, false);
        }
    }

    /**
     * Activities that implement this interface will receive callbacks when the lists change.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Called when the source and destination lists change.
         *
         * @param sourceEntries      the source list
         * @param destinationEntries the destination list
         */
        public void onFragmentInteraction(ArrayList<String> sourceEntries,
                                          ArrayList<String> destinationEntries);
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
        private final Bundle args;

        // Constructors

        /**
         * Creates a new instance of the ListChooserFragment builder.
         */
        public Builder() {
            args = new Bundle();
            args.putStringArrayList(SOURCE_LIST_KEY, new ArrayList<String>());
            args.putStringArrayList(DESTINATION_LIST_KEY, new ArrayList<String>());
        }

        // Static methods
        // Instance methods

        /**
         * Creates a {@link com.sailbravado.androiduilibrary.ListChooserFragment} with the arguments
         * supplied to this builder.
         * @return The ListChooserFragment
         */
        public ListChooserFragment create() {
            ListChooserFragment fragment = new ListChooserFragment();
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * Sets the source list for the ListChooserFragment to be created.  The default is an empty
         * list.
         * @param sourceList The source list to use.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setSourceList(ArrayList<String> sourceList) {
            args.putStringArrayList(SOURCE_LIST_KEY, sourceList);
            return this;
        }

        /**
         * Sets the source list label for the ListChooserFragment to be created.  The default is
         * no label.
         * @param label The label to use.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setSourceListLabel(String label) {
            args.putString(SOURCE_LIST_LABEL_KEY, label);
            return this;
        }

        /**
         * Sets whether the source list shows buttons to allow for sorting the order of the list.
         * The default is {@link #SOURCE_LIST_IS_SORTABLE_DEFAULT}.  Setting setAutoSortSourceList()
         * to true overrides this setting.
         * @param isSortable If <code>true</code>, display sorting buttons.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setSourceListIsSortable(boolean isSortable) {
            args.putBoolean(SOURCE_LIST_IS_SORTABLE_KEY, isSortable);
            return this;
        }

        /**
         * Sets whether the source list automatically sorts when changed.  The default is
         * {@link #AUTO_SORT_SOURCE_LIST_DEFAULT}.  This setting overrides setSourceListIsSortable().
         * @param autoSort If <code>true</code>, automatically sort the source list.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setAutoSortSourceList(boolean autoSort) {
            args.putBoolean(AUTO_SORT_SOURCE_LIST_KEY, autoSort);
            return this;
        }

        /**
         * Sets the destination list for the ListChooserFragment.  The default is an empty list.
         * @param destinationList The destination list to use.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setDestinationList(ArrayList<String> destinationList) {
            args.putStringArrayList(DESTINATION_LIST_KEY, destinationList);
            return this;
        }

        /**
         * Sets the destination list label for the ListChooserFragment to be created.  The default
         * is no label.
         * @param label The label to use.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setDestinationListLabel(String label) {
            args.putString(DESTINATION_LIST_LABEL_KEY, label);
            return this;
        }

        /**
         * Sets whether the destination list shows buttons to allow for sorting the order of the
         * list.  The default is {@link #DESTINATION_LIST_IS_SORTABLE_DEFAULT}.  Setting
         * setAutoSortDestinationList() to true overrides this setting.
         * @param isSortable If <code>true</code>, display sorting buttons.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setDestinationListIsSortable(boolean isSortable) {
            args.putBoolean(DESTINATION_LIST_IS_SORTABLE_KEY, isSortable);
            return this;
        }

        /**
         * Sets whether the destination list automatically sorts when changed.  The default is
         * {@link #AUTO_SORT_DESTINATION_LIST_DEFAULT}.  This setting overrides
         * setDestinationListIsSortable().
         * @param autoSort If <code>true</code>, automatically sort the destination list.
         * @return This {@link com.sailbravado.androiduilibrary.ListChooserFragment.Builder} object
         * to use in chaining calls to set methods.
         */
        public Builder setAutoSortDestinationList(boolean autoSort) {
            args.putBoolean(AUTO_SORT_DESTINATION_LIST_KEY, autoSort);
            return this;
        }

        // Static inner classes
        // Inner classes
    }
}
