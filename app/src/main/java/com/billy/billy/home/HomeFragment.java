package com.billy.billy.home;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.billy.billy.R;

public class HomeFragment extends Fragment {
    private HomeViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        initObserves(textView);
        textView.setOnClickListener(v -> viewModel.onCameraButtonClicked());
        return root;
    }

    public void initObserves(TextView textView) {
        viewModel.getText().observe(getViewLifecycleOwner(), textView::setText);  // todo delete
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
                    viewModel.onResults();
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
