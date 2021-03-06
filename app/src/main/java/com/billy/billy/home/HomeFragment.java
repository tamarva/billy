package com.billy.billy.home;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.billy.billy.BuildConfig;
import com.billy.billy.R;
import com.billy.billy.connections.Endpoint;
import com.billy.billy.sessions.SessionItem;
import com.billy.billy.sessions.SessionState;
import com.google.common.base.Preconditions;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private HomeViewModel viewModel;
    private SessionStateAdapter sessionStateAdapter;
    private DiscoveredEndpointsListAdapter discoveredEndpointsListAdapter;
    private RecyclerView discoveredDevicesRecyclerView;
    private RecyclerView billItemsRecyclerView;
    private ProgressBar progressBar;
    private TextView priceTextView;

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
        discoveredDevicesRecyclerView = rootView.findViewById(R.id.home_fragment_discovered_devices_rv);
        discoveredDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        discoveredEndpointsListAdapter = new DiscoveredEndpointsListAdapter();
        discoveredDevicesRecyclerView.setAdapter(discoveredEndpointsListAdapter);

        billItemsRecyclerView = rootView.findViewById(R.id.home_fragment_bill_items_rv);
        billItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        sessionStateAdapter = new SessionStateAdapter();
        billItemsRecyclerView.setAdapter(sessionStateAdapter);

        rootView.findViewById(R.id.home_fragment_main_bill_button)
                .setOnClickListener(view -> {
                    if (viewModel.shouldScanOnClick()) {
                        scanBill();
                    } else {
                        sendEmail();
                    }
                });

        progressBar = rootView.findViewById(R.id.home_fragment_progress_bar);
        priceTextView = rootView.findViewById(R.id.home_fragment_bill_price);

        if (BuildConfig.DEBUG) {
            View loadSampleDataButton = rootView.findViewById(R.id.home_fragment_sample_data_button);
            loadSampleDataButton.setVisibility(View.VISIBLE);
            loadSampleDataButton.setOnClickListener(view -> {
                viewModel.onCreateSampleData();
                loadSampleDataButton.setVisibility(View.INVISIBLE);
            });
        }
    }

    private void scanBill() {
        CropImage.activity()
                .start(requireContext(), this);
    }

    private void sendEmail() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, viewModel.getSessionSummary());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, getString(R.string.share));
        startActivity(shareIntent);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        observeUpdatesFromViewModel();
    }

    private void observeUpdatesFromViewModel() {
        observeActions();
        observeDiscoveredEndpoints();
        observeBillItems();
        observeCurrentSessionStatus();
        observeShouldShowProgressBar();
        observePrice();
    }

    private void observeActions() {
        viewModel.getAction().observe(getViewLifecycleOwner(), action -> {
            if (action != null) {
                action.run(this);
            }
        });
    }

    private void observeDiscoveredEndpoints() {
        viewModel.getDiscoveredEndpoints().observe(getViewLifecycleOwner(), discoveredEndpoints -> {
            discoveredEndpointsListAdapter.submitList(discoveredEndpoints);
            discoveredEndpointsListAdapter.notifyDataSetChanged();
        });
    }

    private void observeBillItems() {
        viewModel.getSessionStateLiveData().observe(getViewLifecycleOwner(), sessionState -> {
            sessionStateAdapter.submitList(sessionState.getSessionItems());
            sessionStateAdapter.notifyDataSetChanged();
        });
    }

    private void observeCurrentSessionStatus() {
        viewModel.getButtonCaptionStringResLiveData().observe(getViewLifecycleOwner(), captionStringRes -> {
            getView().<TextView>findViewById(R.id.home_fragment_main_bill_button)
                    .setText(captionStringRes);

            if (captionStringRes == R.string.scan) {
                discoveredDevicesRecyclerView.setVisibility(View.VISIBLE);
                billItemsRecyclerView.setVisibility(View.INVISIBLE);
                priceTextView.setVisibility(View.INVISIBLE);
            } else {
                discoveredDevicesRecyclerView.setVisibility(View.INVISIBLE);
                billItemsRecyclerView.setVisibility(View.VISIBLE);
                priceTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void observeShouldShowProgressBar() {
        viewModel.getShouldShowProgressBarLiveData().observe(getViewLifecycleOwner(),
                shouldShow -> progressBar.setVisibility(shouldShow ? View.VISIBLE : View.GONE));
    }


    private void observePrice() {
        viewModel.getPriceLiveData().observe(getViewLifecycleOwner(), price ->
                priceTextView.setText(getString(R.string.price_text, price)));
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
                    Toast.makeText(requireContext(), R.string.error_msg, Toast.LENGTH_LONG).show();
                    requireActivity().finish();
                    return;
                }
            }
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

    private class SessionStateAdapter extends ListAdapter<SessionItem, SessionStateAdapter.SessionStateViewHolder> {
        private class SessionStateViewHolder extends RecyclerView.ViewHolder {
            private final TextView sessionItemTextView;

            public SessionStateViewHolder(@NonNull View itemView) {
                super(itemView);
                sessionItemTextView = itemView.findViewById(R.id.bill_item_title);
            }

            public void bindTo(int position, @NonNull SessionItem sessionItem) {
                Preconditions.checkNotNull(sessionItem);

                String billItemText = sessionItem.getItemName() + " $" + sessionItem.getItemPrice();

                int numParticipantsSelected = sessionItem.getOrderingParticipants().size();
                if (numParticipantsSelected > 0) {
                    billItemText += "\t(";
                    for (String participant : sessionItem.getOrderingParticipants()) {
                        billItemText += SessionState.normalizeName(participant) + ", ";
                    }
                    billItemText = billItemText.substring(0, billItemText.length() - 2) + ")";  // Replace last ", " with ")"
                    sessionItemTextView.setText(billItemText);
                } else {
                    SpannableString underlineText = new SpannableString(billItemText);
                    underlineText.setSpan(new UnderlineSpan(), 0, underlineText.length(), 0);
                    sessionItemTextView.setText(underlineText);
                }

                if (sessionItem.doesContainParticipant(viewModel.getOwnName())) {
                    if (numParticipantsSelected > 1) {
                        sessionItemTextView.setTypeface(null, Typeface.ITALIC);
                    } else {
                        sessionItemTextView.setTypeface(null, Typeface.BOLD_ITALIC);
                    }
                } else {
                    sessionItemTextView.setTypeface(null, Typeface.NORMAL);
                }

                sessionItemTextView.setOnClickListener(view -> viewModel.onBillItemClicked(position));
                sessionItemTextView.setOnLongClickListener(view -> viewModel.onBillItemLongClicked(position));
            }
        }

        protected SessionStateAdapter() {
            super(SessionItem.DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public SessionStateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bill_item, parent, false);
            return new SessionStateViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SessionStateViewHolder holder, int position) {
            SessionItem sessionItem = getItem(position);
            if (sessionItem != null) {
                holder.bindTo(position, sessionItem);
            }
        }
    }
}
