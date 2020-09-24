package com.billy.billy.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.billy.billy.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class HomeFragment extends Fragment {
    private HomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = (new ViewModelProvider(this)).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        root.findViewById(R.id.scan_bill_button)
                .setOnClickListener(view -> viewModel.onCameraButtonClicked());
        initObserves();
        return root;
    }

    public void initObserves() {
        observeAction(viewModel);
    }

    private void observeAction(HomeViewModel homeViewModel) {
        homeViewModel.getAction().observe(getViewLifecycleOwner(), createActionObserver());
    }

    private Observer<HomeViewModel.Action> createActionObserver() {
        return action -> {
            if (action != null) {
                action.run(this);
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HomeViewModel.REQUEST_TAKE_PHOTO) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    viewModel.onBillScanned();
                    break;
                case Activity.RESULT_CANCELED:
                    viewModel.onCancel();
                    break;
                default:
                    break;
            }
        }
    }
}
