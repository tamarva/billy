package com.billy.billy.email;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.billy.billy.R;
import com.google.android.gms.common.AccountPicker;

import java.util.Arrays;

import static android.app.Activity.RESULT_OK;

public class EmailFragment extends Fragment {
    private static final String TAG = EmailFragment.class.getSimpleName();
    private EmailViewModel emailViewModel;
    private static final int REQUEST_CODE_EMAIL = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sendEmail();
    }

    private void sendEmail() {
        Intent intent =
                AccountPicker.newChooseAccountIntent(
                        new AccountPicker.AccountChooserOptions.Builder()
                                .setAllowableAccountsTypes(Arrays.asList("com.google"))
                                .build());
        startActivityForResult(intent, REQUEST_CODE_EMAIL);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent data) {
        if (requestCode == REQUEST_CODE_EMAIL) {
            if (resultCode == RESULT_OK) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                saveToSP(accountName);
            } else {
                showDialog();
            }
        }
    }

    public void showDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Please enter your email")
                .setMessage("For getting your bill on email, please enter your email")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> sendEmail())
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public void saveToSP(String accountName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("email_key", accountName);
        editor.apply();
    }
}