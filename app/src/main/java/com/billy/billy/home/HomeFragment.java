package com.billy.billy.home;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.billy.billy.R;
import com.billy.billy.connections.Endpoint;
import com.billy.billy.text_recognition.BillItem;
import com.google.common.base.Preconditions;
import com.theartofdev.edmodo.cropper.CropImage;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private HomeViewModel viewModel;
    private TextView history;
    private BillItemsListAdapter billItemsAdapter;
    private DiscoveredEndpointsListAdapter discoveredEndpointsListAdapter;

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
        RecyclerView recyclerViewEndpoints = rootView.findViewById(R.id.home_fragment_discovered_devices_rv);
        RecyclerView recyclerViewBill = rootView.findViewById(R.id.home_fragment_bill_items_rv);
        recyclerViewEndpoints.setLayoutManager(new LinearLayoutManager(requireContext()));
        discoveredEndpointsListAdapter = new DiscoveredEndpointsListAdapter();
        recyclerViewEndpoints.setAdapter(discoveredEndpointsListAdapter);
        billItemsAdapter = new BillItemsListAdapter();
        recyclerViewBill.setAdapter(billItemsAdapter);

        history = rootView.findViewById(R.id.home_fragment_history);
        history.setOnClickListener(view -> viewModel.tomer());

        rootView.findViewById(R.id.home_fragment_scan_bill_button)
                .setOnClickListener(view -> onChooseFile());
    }

    private void onChooseFile() {
        CropImage.activity()
                .start(requireContext(), this);
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
        observeBillItems();
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
            discoveredEndpointsListAdapter.submitList(discoveredEndpoints);
            discoveredEndpointsListAdapter.notifyDataSetChanged();
        });
    }

    private void observeBillItems() {
        viewModel.getBillLiveData().observe(getViewLifecycleOwner(), bill -> {
            Log.d(TAG, "observeBillItems");
            if (!bill.billItems().isEmpty()){
                billItemsAdapter.submitList(bill.billItems());
                billItemsAdapter.notifyDataSetChanged();
            }
            else{
                Toast.makeText(requireContext(), R.string.error_msg, Toast.LENGTH_LONG).show();
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
                Log.e(TAG, "onActivityResult: failed to crop image", error);
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
            private final TextView billItem;

            public BillItemViewHolder(@NonNull View itemView) {
                super(itemView);
                billItem = itemView.findViewById(R.id.bill_item_title);
            }

            public void bindTo(@NonNull BillItem billItem) {
                Preconditions.checkNotNull(billItem);
                String billItemText = billItem.name() + billItem.amount() + billItem.price() + billItem.total();
                this.billItem.setText(billItemText);
                this.billItem.setOnClickListener(view -> viewModel.onBillItemClicked(billItem));
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
