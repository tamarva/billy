package com.billy.billy.bill;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;

import com.billy.billy.R;

public class BillFragment extends Fragment {
    private BillViewModel billViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        billViewModel =
                ViewModelProviders.of(this).get(BillViewModel.class);
        View root = inflater.inflate(R.layout.fragment_bill, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);
        billViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}
