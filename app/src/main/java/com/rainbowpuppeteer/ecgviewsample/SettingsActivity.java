package com.rainbowpuppeteer.ecgviewsample;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.rainbowpuppeteer.ecgview.ECGView;
import com.rainbowpuppeteer.ecgview.Graph;

import java.util.Random;

public class SettingsActivity extends AppCompatActivity {

    private ECGView mECGView;
    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        mECGView = (ECGView) findViewById(R.id.ecg_graph);
        mSettingsFragment = new SettingsFragment();
        mECGView.getGraph().enableAutoInvalidate(true);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, mSettingsFragment)
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        ECGView mECGView;
        EditTextPreference mGraphTitle;
        ListPreference mXLabel;
        ListPreference mYLabel;
        SwitchPreferenceCompat mHideLargeGrids;
        SwitchPreferenceCompat mHideSmallGrids;
        SwitchPreferenceCompat mHideVertical;
        SwitchPreferenceCompat mHideHorizontal;
        SwitchPreferenceCompat mSimulationData;

        boolean sim = false;
        Handler mHandler = new Handler();
        Runnable mTimer;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            mECGView = ((SettingsActivity)getActivity()).mECGView;

            mGraphTitle = (EditTextPreference)findPreference("graph_title");
            mECGView.setTitle(mGraphTitle.getText());
            mGraphTitle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newTitle = newValue.toString();
                    mECGView.setTitle(newTitle);
                    mGraphTitle.setText(newTitle);
                    return true;
                }
            });

            mXLabel = (ListPreference) findPreference("x_label");
            mXLabel.setValue("None");
            mXLabel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String position = newValue.toString();
                    mXLabel.setValue(position);
                    if (position.equals("None")) {
                        mECGView.getGraph().setXLabelPosition(Graph.XLabelPosition.NONE, false);
                    } else if (position.equals("Bottom")) {
                        mECGView.getGraph().setXLabelPosition(Graph.XLabelPosition.BOTTOM, true);
                    } else {
                        mECGView.getGraph().setXLabelPosition(Graph.XLabelPosition.TOP, true);
                    }
                    return false;
                }
            });

            mYLabel = (ListPreference) findPreference("y_label");
            mYLabel.setValue("None");
            mYLabel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String position = newValue.toString();
                    mYLabel.setValue(position);
                    if (position.equals("None")) {
                        mECGView.getGraph().setYLabelPosition(Graph.YLabelPosition.NONE, false);
                    } else if (position.equals("Left")) {
                        mECGView.getGraph().setYLabelPosition(Graph.YLabelPosition.LEFT, true);
                    } else {
                        mECGView.getGraph().setYLabelPosition(Graph.YLabelPosition.RIGHT, true);
                    }
                    return false;
                }
            });

            mHideLargeGrids = (SwitchPreferenceCompat) findPreference("hide_large_grids");
            if (mHideLargeGrids.isChecked()) {
                mECGView.getGraph().showLargeGrids(false);
            }
            mHideLargeGrids.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mHideLargeGrids.isChecked()) {
                        mECGView.getGraph().showLargeGrids(true);
                    } else {
                        mECGView.getGraph().showLargeGrids(false);
                    }
                    return true;
                }
            });
            mHideSmallGrids = (SwitchPreferenceCompat) findPreference("hide_small_grids");
            if (mHideSmallGrids.isChecked()) {
                mECGView.getGraph().showGrids(false);
            }
            mHideSmallGrids.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mHideSmallGrids.isChecked()) {
                        mECGView.getGraph().showGrids(true);
                    } else {
                        mECGView.getGraph().showGrids(false);
                    }
                    return true;
                }
            });

            mHideVertical = (SwitchPreferenceCompat) findPreference("hide_vertical");
            if (mHideVertical.isChecked()) {
                mECGView.getGraph().showVertical(false);
            }
            mHideVertical.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mHideVertical.isChecked()) {
                        mECGView.getGraph().showVertical(true);
                    } else {
                        mECGView.getGraph().showVertical(false);
                    }
                    return true;
                }
            });

            mHideHorizontal = (SwitchPreferenceCompat) findPreference("hide_horizontal");
            if (mHideVertical.isChecked()) {
                mECGView.getGraph().showHorizontal(false);
            }
            mHideHorizontal.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mHideHorizontal.isChecked()) {
                        mECGView.getGraph().showHorizontal(true);
                    } else {
                        mECGView.getGraph().showHorizontal(false);
                    }
                    return true;
                }
            });

            mSimulationData = (SwitchPreferenceCompat) findPreference("simulation_data");
            mSimulationData.setChecked(false);
            mSimulationData.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mSimulationData.isChecked()) {
                        sim = false;
                    } else {
                        sim = true;
                    }
                    return true;
                }
            });
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
        }

        @Override
        public void onResume() {
            super.onResume();
            mTimer = new Runnable() {
                @Override
                public void run() {
                    if (sim) {
                        mECGView.getDateSeries().appendDataPoint(getData(), 0.02f, true);
                    }
                    mHandler.postDelayed(this, 200);
                }
            };
            mHandler.postDelayed(mTimer, 200);
        }

        @Override
        public void onPause() {
            super.onPause();
            mHandler.removeCallbacks(mTimer);
        }

        int index = 0;
        double[] data = {
                41, 41.5, 42, 43, 42.5, 42, 42
        };

        double getData() {
            if (index == 7) index = 0;
            return data[index++];
        }
    }
}