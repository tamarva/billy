package com.billy.billy.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.billy.billy.R;
import com.billy.billy.connections.Endpoint;
import com.billy.billy.email.EmailFragment;
import com.billy.billy.text_recognition.BillItem;
import com.google.common.base.Preconditions;
import com.theartofdev.edmodo.cropper.CropImage;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    public static final String EMAIL_KEY = "email_key";
    private HomeViewModel viewModel;
    private TextView history;
    private BillItemsListAdapter billItemAdapter;
    private DiscoveredEndpointsListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = (new ViewModelProvider(this)).get(HomeViewModel.class);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViews(view);
    }

    private void setUpViews(@NonNull View rootView) {
        RecyclerView recyclerView = rootView.findViewById(R.id.home_fragment_discovered_devices_rv);
        RecyclerView recyclerViewBill = rootView.findViewById(R.id.home_fragment_bill_items_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DiscoveredEndpointsListAdapter();
        recyclerView.setAdapter(adapter);
        billItemAdapter = new BillItemsListAdapter();
        recyclerViewBill.setAdapter(billItemAdapter);
        Button emailButton = rootView.findViewById(R.id.home_fragment_send_email_button);


        rootView.findViewById(R.id.home_fragment_scan_bill_button).setOnClickListener(view -> {
            emailButton.setVisibility(View.INVISIBLE);
        });

        sendEmailButton((rootView), emailButton);
        history = rootView.findViewById(R.id.home_fragment_history);
        history.setOnClickListener(view -> viewModel.tomer());

        rootView.findViewById(R.id.home_fragment_scan_bill_button)
                .setOnClickListener(view -> onChooseFile());
    }

    private void onChooseFile() {
        CropImage.activity()
                .start(requireContext(), this);
    }

    private void sendEmailButton(View rootView, Button emailButton){
        View billView = rootView.findViewById(R.id.home_fragment_bill_items_rv);
        if (billView.getVisibility() == View.VISIBLE){
        emailButton.setVisibility(View.VISIBLE);
        emailButton.setOnClickListener(view -> sendEmail());
        }
    }

    private void sendEmail() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (sp.getString(EMAIL_KEY, "").equals("")){
            Fragment emailFragment = new EmailFragment();
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.home_fragment_recycler_view_wrapper, emailFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        observeUpdatesFromViewModel();
    }

    private void observeUpdatesFromViewModel() {
        observeActions();
        observeHistory();
        observeDiscoveredEndpoints();
    }

    private void observeActions() {
        viewModel.getAction().observe(getViewLifecycleOwner(), action -> {
            if (action != null) {
                action.run(this);
            }
        });
    }

    private void observeHistory() {
        viewModel.getHistory().observe(getViewLifecycleOwner(), newHistory -> history.setText(newHistory));
    }

    private void observeDiscoveredEndpoints() {
        viewModel.getDiscoveredEndpoints().observe(getViewLifecycleOwner(), discoveredEndpoints -> {
            adapter.submitList(discoveredEndpoints);
            adapter.notifyDataSetChanged();
        });
    }

    private void observeBillItems() {
        Log.d(TAG, "observeBillItems");
        viewModel.getMyBill().observe(getViewLifecycleOwner(), bill -> {
            if (!bill.billItems().isEmpty()){
                billItemAdapter.submitList(bill.billItems());
                billItemAdapter.notifyDataSetChanged();
            }
            else{
                Toast.makeText(requireContext(),"please re-scan bill", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri imageUri = result.getUri();
                viewModel.onBillScanned(imageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.e(TAG, "onActivityResult: failed to crop image ", error);
                Toast.makeText(requireContext(), "failed to crop image, please retry", Toast.LENGTH_LONG).show();
            }
        }
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == HomeViewModel.REQUEST_CONNECTION_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    getActivity().finish();
                    return;
                }
            }
            getActivity().recreate();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.onStop();
    }

    private class DiscoveredEndpointsListAdapter
            extends ListAdapter<Endpoint, DiscoveredEndpointsListAdapter.DiscoveredEndpointViewHolder> {
        private class DiscoveredEndpointViewHolder extends RecyclerView.ViewHolder {
            private final TextView name;

            public DiscoveredEndpointViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.discovered_endpoint_item_name);
            }

            public void bindTo(@NonNull Endpoint discoveredEndpoint) {
                Preconditions.checkNotNull(discoveredEndpoint);

                name.setText(discoveredEndpoint.getName());
                name.setOnClickListener(view -> viewModel.onDiscoveredEndpointClicked(discoveredEndpoint));
            }
        }

        protected DiscoveredEndpointsListAdapter() {
            super(Endpoint.DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public DiscoveredEndpointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.discovered_endpoint_item, parent, false);
            return new DiscoveredEndpointViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DiscoveredEndpointViewHolder holder, int position) {
            Endpoint discoveredEndpoint = getItem(position);
            if (discoveredEndpoint != null) {
                holder.bindTo(discoveredEndpoint);
            }
        }
    }
    private class BillItemsListAdapter
            extends ListAdapter<BillItem, BillItemsListAdapter.BillItemViewHolder> {
        private class BillItemViewHolder extends RecyclerView.ViewHolder {
            private final TextView name;

            public BillItemViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.discovered_endpoint_item_name);
            }

            public void bindTo(@NonNull BillItem billItem) {
                Preconditions.checkNotNull(billItem);
                name.setText(billItem.name());
//                name.setOnClickListener(view -> viewModel.onBillItemClicked(billItem));
            }
        }

        protected BillItemsListAdapter() {
            super(BillItem.DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public BillItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bill_item, parent, false);
            return new BillItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BillItemViewHolder holder, int position) {
            BillItem billItem = getItem(position);
            if (billItem != null) {
                holder.bindTo(billItem);
            }
        }
    }
}
