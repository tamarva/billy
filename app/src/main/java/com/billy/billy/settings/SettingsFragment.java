package com.billy.billy.settings;

import static com.google.common.base.Preconditions.checkState;

import android.os.Bundle;

import com.billy.billy.R;
import com.billy.billy.utils.Preferences;
import com.google.common.base.Strings;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        initUserNamePreferences();
    }

    private void initUserNamePreferences() {
        Preference userNameEditTextPreference = findPreference(getString(R.string.settings_user_name_key));
        checkState(userNameEditTextPreference != null);

        String currentUserName = Preferences.Connections.getUserName(requireActivity());
        userNameEditTextPreference.setTitle(getString(R.string.settings_user_name_preferences_title, currentUserName));
        userNameEditTextPreference
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    String newValueAsString = (String) newValue;
                    if (Strings.isNullOrEmpty(newValueAsString)) {
                        return false;
                    }

                    Preferences.Connections.setUserName(requireActivity(), newValueAsString);
                    userNameEditTextPreference.setTitle(
                            getString(R.string.settings_user_name_preferences_title, newValueAsString));
                    return true;
                });

    }
}
