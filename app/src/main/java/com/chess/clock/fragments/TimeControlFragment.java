package com.chess.clock.fragments;

import static android.view.View.GONE;
import static com.chess.clock.util.ClockUtils.getIntOrZero;
import static com.chess.clock.util.ClockUtils.onTimeChangedAction;
import static com.chess.clock.util.ClockUtils.setClockTextWatcherWithCallback;
import static com.chess.clock.util.ClockUtils.twoDecimalPlacesFormat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.chess.clock.R;
import com.chess.clock.dialog.EditStageDialogFragment;
import com.chess.clock.engine.Stage;
import com.chess.clock.engine.TimeControl;
import com.chess.clock.engine.TimeControlWrapper;
import com.chess.clock.engine.TimeIncrement;
import com.chess.clock.entities.AppTheme;
import com.chess.clock.util.AutoNameFormatter;
import com.chess.clock.views.StageRowView;
import com.chess.clock.views.ViewUtils;
import com.google.android.material.tabs.TabLayout;

/**
 * UI fragment to create and edit a TimeControl.
 */
public class TimeControlFragment extends BaseFragment implements EditStageDialogFragment.OnStageEditListener {

    /**
     * Bundle/Instance state keys
     */
    private static final String STATE_TIME_CONTROL_SNAPSHOT_KEY = "time_control_snapshot_key";
    private static final String STATE_ADVANCED_MODE_KEY = "advanced_mode_key";
    private static final String STATE_PLAYER_ONE_KEY = "player_one_key";
    private static final String STATE_AUTO_NAMING_KEY = "auto_naming_key";
    private static final String STATE_LATEST_AUTO_NAME_KEY = "latest_auto_name_key";
    private static final String ARG_EDIT_MODE = "arg_edit_mode";
    /**
     * Dialog Fragment TAGS
     */
    private static final String TAG_EXIT_DIALOG_FRAGMENT = "ExitDialogFragment";
    /**
     * DIALOG request code
     */
    private static final int REQUEST_EXIT_DIALOG = 3;
    /**
     * Activity attached.
     */
    private OnTimeControlListener timeControlListener;
    /**
     * State.
     */
    private TimeControlWrapper timeControlWrapper;
    private TimeControl selectedTimeControl;
    private boolean playerOneSelected = true;
    private boolean advancedMode = false;
    private boolean editMode = false;
    private boolean autoNamingEnabled = true;
    private String latestAutoName = "";

    /**
     * Formatters
     */
    private AutoNameFormatter autoNameFormatter;

    /**
     * This is used to check for modifications before exiting.
     */
    private TimeControlWrapper mTimeControlSnapshot;
    /**
     * UI
     */
    private LinearLayout stagesList;
    private EditText nameEt;
    private EditText minutesEt;
    private EditText secondsEt;
    private EditText incrementMinutesEt;
    private EditText incrementSecondsEt;
    private View copyPlayerOneLay;
    private SwitchCompat copyPLayerOneSwitch;
    private SwitchCompat advancedModeSwitch;
    private View baseView;
    private View advancedView;
    private View addStageView;
    private View addStageDivider;
    private CardView saveButton;
    private TabLayout tabLayout;


    public TimeControlFragment() {
    }

    public static TimeControlFragment newInstance(boolean editMode) {

        Bundle args = new Bundle();
        args.putBoolean(ARG_EDIT_MODE, editMode);

        TimeControlFragment fragment = new TimeControlFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            timeControlListener = (OnTimeControlListener) requireActivity();
            // Fetch current TimeControl object
            timeControlWrapper = timeControlListener.getEditableTimeControl();

        } catch (ClassCastException e) {
            throw new ClassCastException(requireActivity() + " must implement OnTimeControlListener");
        }
    }

    @Override
    void loadTheme(AppTheme theme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorStateList tintList = theme.colorStateListFocused(requireContext());
            minutesEt.setBackgroundTintList(tintList);
            secondsEt.setBackgroundTintList(tintList);
            incrementMinutesEt.setBackgroundTintList(tintList);
            incrementSecondsEt.setBackgroundTintList(tintList);
        }
        ColorStateList tintChecked = theme.colorStateListChecked(requireContext());
        DrawableCompat.setTintList(advancedModeSwitch.getThumbDrawable(), tintChecked);
        DrawableCompat.setTintList(copyPLayerOneSwitch.getThumbDrawable(), tintChecked);
        int mainColor = theme.color(requireContext());
        saveButton.setCardBackgroundColor(mainColor);
        tabLayout.setSelectedTabIndicatorColor(mainColor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.custom_time);
        editMode = requireArguments().getBoolean(ARG_EDIT_MODE);
        View v = inflater.inflate(R.layout.fragment_time_control, container, false);
        stagesList = v.findViewById(R.id.list_stages);
        advancedModeSwitch = v.findViewById(R.id.advancedModeSwitch);
        baseView = v.findViewById(R.id.baseLay);
        advancedView = v.findViewById(R.id.advancedLay);
        copyPlayerOneLay = v.findViewById(R.id.copyPlayerOneLay);
        copyPLayerOneSwitch = v.findViewById(R.id.copyPlayerOneSwitch);
        saveButton = v.findViewById(R.id.saveBtn);
        nameEt = v.findViewById(R.id.time_control_name);
        minutesEt = v.findViewById(R.id.baseMinEt);
        secondsEt = v.findViewById(R.id.baseSecEt);
        incrementMinutesEt = v.findViewById(R.id.baseIncrementMinEt);
        incrementSecondsEt = v.findViewById(R.id.baseIncrementSecEt);
        addStageView = v.findViewById(R.id.addStageTv);
        addStageDivider = v.findViewById(R.id.addStageDivider);
        tabLayout = v.findViewById(R.id.tabLayout);
        if (timeControlWrapper != null) {
            selectedTimeControl = timeControlWrapper.getTimeControlPlayerOne();

            if (savedInstanceState != null) {
                mTimeControlSnapshot = savedInstanceState.getParcelable(STATE_TIME_CONTROL_SNAPSHOT_KEY);
                advancedMode = savedInstanceState.getBoolean(STATE_ADVANCED_MODE_KEY);
                playerOneSelected = savedInstanceState.getBoolean(STATE_PLAYER_ONE_KEY);
                autoNamingEnabled = savedInstanceState.getBoolean(STATE_AUTO_NAMING_KEY);
                latestAutoName = savedInstanceState.getString(STATE_LATEST_AUTO_NAME_KEY);
            } else {
                // Save copy to check modifications before exit.
                mTimeControlSnapshot = null;
                try {
                    mTimeControlSnapshot = (TimeControlWrapper) timeControlWrapper.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Could not build time control snapshot");
                }
            }

            copyPLayerOneSwitch.setChecked(timeControlWrapper.isSameAsPlayerOne());

            String name = selectedTimeControl.getName();
            if (name != null && !name.isEmpty()) {
                nameEt.setText(name);
            }
            loadStages();
        }
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        saveButton.setOnClickListener(v -> saveTimeControl());
        addStageView.setOnClickListener(v -> addNewStage());
        nameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (timeControlWrapper != null && !text.isEmpty()) {
                    timeControlWrapper.getTimeControlPlayerOne().setName(s.toString());
                    timeControlWrapper.getTimeControlPlayerTwo().setName(s.toString());
                }
            }
        });

        autoNameFormatter = new AutoNameFormatter(new AutoNameFormatter.NameParametersFormat() {
            @Override
            public String getMinutesFormatted(int minutes) {
                return getString(R.string.x_min, minutes);
            }

            @Override
            public String getSecondsFormatted(int seconds) {
                return getString(R.string.x_sec, seconds);
            }
        });
        nameEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String currentName = nameEt.getText().toString();
                autoNamingEnabled = currentName.isEmpty() || currentName.equals(latestAutoName);
            }
        });
        onTimeChangedAction(minutesEt, this::autoNaming);
        onTimeChangedAction(incrementMinutesEt, this::autoNaming);
        setClockTextWatcherWithCallback(secondsEt, this::autoNaming);
        setClockTextWatcherWithCallback(incrementSecondsEt, this::autoNaming);

        advancedModeSwitch.setChecked(advancedMode);
        advancedModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            advancedMode = isChecked;
            updateUi();
        });
        copyPLayerOneSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timeControlWrapper.setSameAsPlayerOne(isChecked);
            if (isChecked && !playerOneSelected) {
                showPlayerOneViews();
            }
        });
        copyPlayerOneLay.setVisibility(GONE);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean playerOneTab = tab.getPosition() == 0;
                ViewUtils.showView(copyPlayerOneLay, !playerOneTab);
                playerOneSelected = playerOneTab;
                selectedTimeControl = playerOneTab ? timeControlWrapper.getTimeControlPlayerOne() : timeControlWrapper.getTimeControlPlayerTwo();
                if (!playerOneTab && timeControlWrapper.isSameAsPlayerOne()) {
                    showPlayerOneViews();
                } else {
                    updateStagesDisplay();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        if (editMode) {
            view.findViewById(R.id.advancedModeSwitchLay).setVisibility(View.INVISIBLE);
        }
        if (savedInstanceState == null) {
            String hint = twoDecimalPlacesFormat(0);
            minutesEt.setHint(hint);
            incrementMinutesEt.setHint(hint);
            secondsEt.setHint(hint);
            incrementSecondsEt.setHint(hint);
        } else {
            int tabId = playerOneSelected ? 0 : 1;
            TabLayout.Tab tab = tabLayout.getTabAt(tabId);
            if (tab != null) {
                tab.select();
            }
        }
        updateUi();
    }

    /**
     * Method to set auto name only if name was not set by user and only in simple mode.
     */
    private void autoNaming() {
        if (!autoNamingEnabled || advancedMode || editMode) return;

        int minutes = getIntOrZero(minutesEt);
        int seconds = getIntOrZero(secondsEt);
        int incrementMinutes = getIntOrZero(incrementMinutesEt);
        int incrementSeconds = getIntOrZero(incrementSecondsEt);

        String newName = autoNameFormatter.prepareAutoName(minutes, seconds, incrementMinutes, incrementSeconds);

        if (newName != null) {
            latestAutoName = newName;
            nameEt.setText(newName);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_TIME_CONTROL_SNAPSHOT_KEY, mTimeControlSnapshot);
        outState.putBoolean(STATE_ADVANCED_MODE_KEY, advancedMode);
        outState.putBoolean(STATE_PLAYER_ONE_KEY, playerOneSelected);
        outState.putBoolean(STATE_AUTO_NAMING_KEY, autoNamingEnabled);
        outState.putString(STATE_LATEST_AUTO_NAME_KEY, latestAutoName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        timeControlListener = null;
    }

    private void loadStages() {
        Stage[] stages = selectedTimeControl.getStageManager().getStages();
        boolean showAddStageView = stages.length < Stage.MAX_ALLOWED_STAGES_COUNT;
        ViewUtils.showView(addStageView, showAddStageView);
        ViewUtils.showView(addStageDivider, showAddStageView);
        int i = 0;
        while (i < Stage.MAX_ALLOWED_STAGES_COUNT) {
            StageRowView row = (StageRowView) stagesList.getChildAt(i);
            if (i < stages.length) {
                Stage stage = stages[i];
                row.updateData(i + 1, stage);
                row.setOnClickListener(v -> showStageEditorDialog(stage));
                row.setVisibility(View.VISIBLE);
            } else {
                row.setOnClickListener(null);
                row.setVisibility(View.GONE);
            }
            i++;
        }
    }

    private void updateUi() {
        ViewUtils.showView(baseView, !advancedMode && !editMode);
        ViewUtils.showView(advancedView, advancedMode || editMode);
    }

    private void saveTimeControl() {
        if (timeControlWrapper == null) return;

        hideSoftKeyboard();

        String newControlName = nameEt.getText().toString();
        if (newControlName.equals("")) {
            nameEt.requestFocus();
            Toast.makeText(getActivity(), getString(R.string.toast_requesting_time_control_name), Toast.LENGTH_LONG).show();
        } else if (!timeControlWrapper.bothUsersHaveAtLeastOneStage()) {
            Toast.makeText(getActivity(), getString(R.string.toast_requesting_time_control_stage), Toast.LENGTH_LONG).show();
        } else {
            if (advancedMode || editMode) {
                if (timeControlWrapper.isSameAsPlayerOne()) {
                    TimeControl playerOneClone = null;
                    try {
                        playerOneClone = (TimeControl) timeControlWrapper.getTimeControlPlayerOne().clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    timeControlWrapper.setTimeControlPlayerTwo(playerOneClone);
                }
            } else {
                int minutes = getIntOrZero(minutesEt);
                int seconds = getIntOrZero(secondsEt);
                int incrementMinutes = getIntOrZero(incrementMinutesEt);
                int incrementSeconds = getIntOrZero(incrementSecondsEt);

                long gameDurationMs = minutes * 60 * 1000L + seconds * 1000L;
                long incrementMs = incrementMinutes * 60 * 1000L + incrementSeconds * 1000L;

                if (gameDurationMs == 0) {
                    Toast.makeText(getActivity(), getString(R.string.please_set_time), Toast.LENGTH_LONG).show();
                    return;
                }

                TimeIncrement timeIncrement = new TimeIncrement(TimeIncrement.Type.FISCHER, incrementMs);
                Stage stage = new Stage(0, gameDurationMs, timeIncrement);
                TimeControl simpleControl = new TimeControl(newControlName, new Stage[]{stage});
                timeControlWrapper.setTimeControlPlayerOne(simpleControl);
                timeControlWrapper.setTimeControlPlayerTwo(simpleControl);
            }
            timeControlListener.saveTimeControl();
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void hideSoftKeyboard() {
        nameEt.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(nameEt.getWindowToken(), 0);
    }

    private void updateStagesDisplay() {
        loadStages();
    }

    public void showConfirmGoBackDialog() {
        if (!(timeControlWrapper.isEqual(mTimeControlSnapshot))) {
            DialogFragment newFragment = ExitConfirmationDialogFragment.newInstance();
            newFragment.setTargetFragment(this, REQUEST_EXIT_DIALOG);
            newFragment.show(getParentFragmentManager(), TAG_EXIT_DIALOG_FRAGMENT);
        } else {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void addNewStage() {
        hideSoftKeyboard();
        if (selectedTimeControl.getStageManager().canAddStage()) {
            selectedTimeControl.getStageManager().addNewStage();
            updateStagesDisplay();
        }
    }

    public void removeStage(int stageIndex) {
        selectedTimeControl.getStageManager().removeStage(stageIndex);
        DialogFragment editDialog = (DialogFragment) getChildFragmentManager().findFragmentByTag(EditStageDialogFragment.TAG);
        if (editDialog != null) {
            editDialog.dismissAllowingStateLoss();
        }
        updateStagesDisplay();
    }

    private void showStageEditorDialog(Stage stage) {
        DialogFragment dialogFragment = EditStageDialogFragment.newInstance(stage);
        dialogFragment.show(getChildFragmentManager(), EditStageDialogFragment.TAG);
    }

    /**
     * Clone the player one views if the "same as player one" switch is turned on in the player 2 tab
     */
    private void showPlayerOneViews() {
        TimeControl playerOneClone;
        try {
            playerOneClone = (TimeControl) timeControlWrapper.getTimeControlPlayerOne().clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not clone player one time control");
        }
        timeControlWrapper.setTimeControlPlayerTwo(playerOneClone);
        selectedTimeControl = playerOneSelected ? timeControlWrapper.getTimeControlPlayerOne() : timeControlWrapper.getTimeControlPlayerTwo();

        updateStagesDisplay();
    }

    @Override
    public void onStageEditDone(int stageId, int moves, long timeValue) {
        Stage[] stages = selectedTimeControl.getStageManager().getStages();

        // verify stage was not removed
        if (stages.length <= stageId) return;

        Stage stage = stages[stageId];
        if (stage.getStageType() == Stage.StageType.GAME) {
            stage.setMoves(Stage.GAME_STAGE_MOVES);
        } else {
            stage.setMoves(moves);
        }

        if (stage.getDuration() != timeValue) {
            stage.setDuration(timeValue);
        }

        updateStagesDisplay();
    }

    /**
     * This interface must be implemented by activities that contain this fragment to allow interaction.
     */
    public interface OnTimeControlListener {
        TimeControlWrapper getEditableTimeControl();

        void saveTimeControl();
    }

    /**
     * DIALOG
     */
    public static class ExitConfirmationDialogFragment extends DialogFragment {

        public static ExitConfirmationDialogFragment newInstance() {
            return new ExitConfirmationDialogFragment();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.WhiteButtonsDialogTheme);
            AlertDialog alertDialog = builder
                    .setTitle(getString(R.string.exit_dialog_title))
                    .setMessage(getString(R.string.exit_dialog_message))
                    .setNegativeButton(getString(R.string.exit_dialog_cancel), (arg0, arg1) -> {
                        Fragment target = getTargetFragment();
                        if (target != null) {
                            target.requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    })
                    .setPositiveButton(getString(R.string.exit_dialog_ok), (arg0, arg1) -> {
                        Fragment target = getTargetFragment();
                        if (target != null) {
                            ((TimeControlFragment) target).saveTimeControl();
                        }
                    })
                    .create();
            ViewUtils.setLargePopupMessageTextSize(alertDialog, getResources());
            return alertDialog;
        }
    }
}