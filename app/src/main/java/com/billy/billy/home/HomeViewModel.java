package com.billy.billy.home;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.billy.billy.R;
import com.billy.billy.connections.ConnectionLifecycleListener;
import com.billy.billy.connections.ConnectionRole;
import com.billy.billy.connections.ConnectionsService;
import com.billy.billy.connections.ConnectionsUtils;
import com.billy.billy.connections.Endpoint;
import com.billy.billy.connections.EndpointDiscoveryListener;
import com.billy.billy.connections.PayloadListener;
import com.billy.billy.text_recognition.Bill;
import com.billy.billy.text_recognition.BillItem;
import com.billy.billy.text_recognition.TextRecognition;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class HomeViewModel extends AndroidViewModel {
    private static final String TAG = HomeViewModel.class.getSimpleName();
    static final int REQUEST_CONNECTION_PERMISSIONS = 2;
    @SuppressLint("StaticFieldLeak") private final Context applicationContext;
    private final ConnectionsService connectionService;
    private final MutableLiveData<Action> action = new MutableLiveData<>();
    private final MutableLiveData<List<Endpoint>> discoveredEndpoints = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> history = new MutableLiveData<>("EMPTY");
    private final MutableLiveData<Bill> billLiveData = new MutableLiveData<>();
    private ConnectionRole connectionRole = ConnectionRole.DISCOVERER;

    public HomeViewModel(Application application) {
        super(application);
        applicationContext = application;
        connectionService = new ConnectionsService(applicationContext, createConnectionLifecycleListener(),
                createEndpointDiscoveryListener(), createPayloadListener());
        connectionService.setConnectionRole(connectionRole);
    }

    private ConnectionLifecycleListener createConnectionLifecycleListener() {
        return new ConnectionLifecycleListener() {
            @Override
            public void onConnectionInitiated(@NonNull Endpoint endpoint, @NonNull ConnectionInfo connectionInfo) {
                // TODO: Have better UI.
                action.postValue(fragment -> {
                    new AlertDialog.Builder(fragment.requireActivity(), R.style.BillyAlertDialog)
                            .setTitle(applicationContext.getString(R.string.connections_connection_initiated_dialog_title,
                                    endpoint.getName()))
                            .setMessage(applicationContext.getString(R.string.connections_connection_initiated_dialog_title,
                                    connectionInfo.getAuthenticationToken()))
                            .setPositiveButton(R.string.connections_connection_initiated_dialog_positive_button,
                                    (DialogInterface dialog, int which) -> connectionService.acceptConnection(endpoint))
                            .setNegativeButton(R.string.connections_connection_initiated_dialog_negative_button,
                                    (DialogInterface dialog, int which) -> connectionService.rejectConnection(endpoint))
                            .setIcon(android.R.drawable.ic_input_add)
                            .show();
                });
            }

            @Override
            public void onConnectionResult(@NonNull Endpoint endpoint, @NonNull ConnectionResolution result) {
                if (result.getStatus().getStatusCode() != ConnectionsStatusCodes.STATUS_OK) {
                    String message =
                            applicationContext.getString(R.string.connections_connection_failed, endpoint.getName());
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
                    return;
                }

                String message =
                        applicationContext.getString(R.string.connections_connection_success, endpoint.getName());
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
                if (connectionRole.equals(ConnectionRole.ADVERTISER)) {
                    syncNewEndpoint(endpoint);
                }
            }

            @Override
            public void onDisconnected(@NonNull Endpoint endpoint) {
                String message =
                        applicationContext.getString(R.string.connections_connection_lost, endpoint.getName());
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
                // TODO: If discoverer and discoveredEndpoints.getValue() empty show dialog asking if want to start advertising.
            }
        };
    }

    private void syncNewEndpoint(@NonNull Endpoint endpoint) {
        connectionService.sendString(history.getValue(), Collections.singleton(endpoint));
    }

    private EndpointDiscoveryListener createEndpointDiscoveryListener() {
        return new EndpointDiscoveryListener() {
            @Override
            public void onEndpointFound(@NonNull Endpoint endpoint, @NonNull DiscoveredEndpointInfo info) {
                List<Endpoint> currentList = discoveredEndpoints.getValue();
                currentList.add(endpoint);
                discoveredEndpoints.setValue(currentList);
            }

            @Override
            public void onEndpointLost(@NonNull Endpoint endpoint) {
                List<Endpoint> currentList = discoveredEndpoints.getValue();
                currentList.remove(endpoint);
                discoveredEndpoints.setValue(currentList);
            }
        };
    }

    private PayloadListener createPayloadListener() {
        return new PayloadListener() {
            @Override
            public void onPayloadReceived(@NonNull Endpoint endpoint, @NonNull Payload payload) {
                if (connectionRole.equals(ConnectionRole.ADVERTISER)) {
                    String currentHistory = history.getValue();
                    currentHistory += " " + new String(payload.asBytes());
                    history.setValue(currentHistory);
                    connectionService.sendString(currentHistory);
                } else {
                    history.setValue(new String(payload.asBytes()));
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull Endpoint endpoint, @NonNull PayloadTransferUpdate update) {
                Log.d(TAG, "Received update, ignoring it.");
            }
        };
    }

    public LiveData<Action> getAction() {
        return action;
    }

    public LiveData<String> getHistory() {
        return history;
    }

    public LiveData<List<Endpoint>> getDiscoveredEndpoints() {
        return discoveredEndpoints;
    }

    public LiveData<Bill> getBillLiveData() {
        return billLiveData;
    }

    public void onBillScanned(Uri imageUri) {
        TextRecognition textRecognition = new TextRecognition();
        textRecognition.detectText(getApplication(), imageUri,
                bill -> {
                    Log.d(TAG, "Got result: " + bill.toString());
                    history.setValue(bill.toString());
                    billLiveData.setValue(bill);
                });

        connectionRole = ConnectionRole.ADVERTISER;
        connectionService.setConnectionRole(connectionRole);
    }

    public void onStart() {
        if (!hasPermissions(applicationContext, getRequiredPermissions())) {
            action.setValue(fragment ->
                    fragment.requestPermissions(getRequiredPermissions(), REQUEST_CONNECTION_PERMISSIONS));
        }
        connectionService.setConnectionRole(connectionRole);
    }

    private String[] getRequiredPermissions() {
        String[] permissions = ConnectionsUtils.getRequiredPermissions();
        return permissions;
    }

    public void onStop() {
        connectionService.reset();
        /*
         * todo tomer:
         *  1. When the app goes to the background we disconnect all the connections.
         * Instead, we can decide on a unique password and share in on session start, and use it for quickly accepting in onConnectionInitiated().
         *
         * 2. Have a way to propagate errors (rejection failed, starting to advertising failed etc)
         */
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void onDiscoveredEndpointClicked(Endpoint discoveredEndpoint) {
        checkState(connectionRole == ConnectionRole.DISCOVERER);

        connectionService.requestConnectionToDiscoveredEndpoint(discoveredEndpoint);
    }

    public void onBillItemClicked(BillItem billItem){
        Log.d(TAG, "onBillItemClicked: ");
    }

    public void tomer() {
        if (connectionRole.equals(ConnectionRole.ADVERTISER)) {
            String currentHistory = history.getValue();
            currentHistory += " ADV";
            history.setValue(currentHistory);
            connectionService.sendString(currentHistory);
        } else {
            connectionService.sendString("DIS");
        }
    }

    public interface Action {
        void run(Fragment fragment);
    }
}
