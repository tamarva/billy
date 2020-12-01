package com.billy.billy.home;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

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
import com.billy.billy.sessions.Selection;
import com.billy.billy.sessions.SessionState;
import com.billy.billy.text_recognition.TextRecognition;
import com.billy.billy.utils.Preferences;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;

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
    private final Gson gson = new Gson();
    private final ConnectionsService connectionService;
    private final MutableLiveData<Action> action = new MutableLiveData<>();
    private final MutableLiveData<List<Endpoint>> discoveredEndpoints = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<SessionState> sessionStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> buttonCaptionStringResLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> shouldShowProgressBarLiveData = new MutableLiveData<>(true);
    private ConnectionRole connectionRole = ConnectionRole.DISCOVERER;

    public HomeViewModel(Application application) {
        super(application);
        applicationContext = application;
        connectionService = new ConnectionsService(applicationContext, createConnectionLifecycleListener(),
                createEndpointDiscoveryListener(), createPayloadListener());
        connectionService.setConnectionRole(connectionRole);

        buttonCaptionStringResLiveData.setValue(R.string.scan);
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
                            .setMessage(applicationContext.getString(R.string.connections_connection_initiated_dialog_message,
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
                buttonCaptionStringResLiveData.setValue(R.string.share);
                if (connectionRole.equals(ConnectionRole.ADVERTISER)) {
                    syncNewEndpoint(endpoint);
                }
            }

            @Override
            public void onDisconnected(@NonNull Endpoint endpoint) {
                String message =
                        applicationContext.getString(R.string.connections_connection_lost, endpoint.getName());
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();

                if (connectionRole == ConnectionRole.DISCOVERER) {
                    buttonCaptionStringResLiveData.setValue(R.string.scan);
                }
            }
        };
    }

    private void syncNewEndpoint(@NonNull Endpoint endpoint) {
        SessionState currentSessionState = sessionStateLiveData.getValue();
        if (currentSessionState == null) {
            Log.e(TAG, "Couldn't update that a new endpoint was added - sessionState is null.");
            return;
        }

        SessionState newSessionState = currentSessionState.withNewParticipant(endpoint.getFullName());
        sessionStateLiveData.setValue(newSessionState);
        connectionService.sendString(gson.toJson(newSessionState));
    }

    private void syncNewSelection(@NonNull Selection selection) {
        SessionState currentSessionState = sessionStateLiveData.getValue();
        if (currentSessionState == null) {
            Log.e(TAG, "Couldn't sync new selection - sessionState is null.");
            return;
        }

        SessionState newSessionState = currentSessionState.withSelection(selection);
        sessionStateLiveData.setValue(newSessionState);
        connectionService.sendString(gson.toJson(newSessionState));
    }

    private EndpointDiscoveryListener createEndpointDiscoveryListener() {
        return new EndpointDiscoveryListener() {
            @Override
            public void onEndpointFound(@NonNull Endpoint endpoint, @NonNull DiscoveredEndpointInfo info) {
                List<Endpoint> currentList = discoveredEndpoints.getValue();
                currentList.add(endpoint);
                discoveredEndpoints.setValue(currentList);

                if (connectionRole == ConnectionRole.DISCOVERER && currentList.size() > 0) {
                    shouldShowProgressBarLiveData.setValue(false);
                }
            }

            @Override
            public void onEndpointLost(@NonNull Endpoint endpoint) {
                List<Endpoint> currentList = discoveredEndpoints.getValue();
                currentList.remove(endpoint);
                discoveredEndpoints.setValue(currentList);

                if (connectionRole == ConnectionRole.DISCOVERER && currentList.size() == 0) {
                    shouldShowProgressBarLiveData.setValue(true);
                }
            }
        };
    }

    private PayloadListener createPayloadListener() {
        return new PayloadListener() {
            @Override
            public void onPayloadReceived(@NonNull Endpoint endpoint, @NonNull Payload payload) {
                if (connectionRole.equals(ConnectionRole.ADVERTISER)) {
                    String json = new String(payload.asBytes());
                    Selection selection = gson.fromJson(json, Selection.class);
                    syncNewSelection(selection);
                } else {
                    String json = new String(payload.asBytes());
                    SessionState sessionState = gson.fromJson(json, SessionState.class);
                    sessionStateLiveData.setValue(sessionState);
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

    public LiveData<List<Endpoint>> getDiscoveredEndpoints() {
        return discoveredEndpoints;
    }

    public LiveData<SessionState> getSessionStateLiveData() {
        return sessionStateLiveData;
    }

    public LiveData<Integer> getButtonCaptionStringResLiveData() {
        return buttonCaptionStringResLiveData;
    }

    public LiveData<Boolean> getShouldShowProgressBarLiveData() {
        return shouldShowProgressBarLiveData;
    }

    public void onBillScanned(@NonNull Uri imageUri) {
        shouldShowProgressBarLiveData.setValue(true);

        TextRecognition textRecognition = new TextRecognition();
        textRecognition.detectText(getApplication(), imageUri,
                bill -> {
                    Log.d(TAG, "Got result: " + bill.toString());
                    List<String> participants = singletonList(getOwnName());
                    sessionStateLiveData.setValue(SessionState.create(participants, bill));
                    shouldShowProgressBarLiveData.setValue(false);
                    buttonCaptionStringResLiveData.setValue(R.string.share);
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

    public void onDiscoveredEndpointClicked(@Nonnull Endpoint discoveredEndpoint) {
        checkState(connectionRole == ConnectionRole.DISCOVERER);

        connectionService.requestConnectionToDiscoveredEndpoint(discoveredEndpoint);
    }

    public void onBillItemClicked(int index) {
        Log.d(TAG, "onBillItemClicked");

        selectItem(index, true);
    }

    public boolean onBillItemLongClicked(int index) {
        Log.d(TAG, "onBillItemLongClicked");

        selectItem(index, false);
        return true;
    }

    private void selectItem(int index, boolean isSelecting) {
        Selection selection = Selection.create(getOwnName(), index, isSelecting);

        if (connectionRole == ConnectionRole.ADVERTISER) {
            syncNewSelection(selection);
        } else {
            connectionService.sendString(gson.toJson(selection));
        }
    }

    private String getOwnName() {
        return Preferences.Connections.getUserUniqueID(applicationContext);
    }

    public boolean shouldScanOnClick() {
        Integer currentCaptionStringRes = buttonCaptionStringResLiveData.getValue();
        return currentCaptionStringRes != null && currentCaptionStringRes == R.string.scan;
    }

    private String getCurrentDate() {
        Date now = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        return df.format(now);
    }

    public String getSessionSummary() {
        SessionState sessionState = sessionStateLiveData.getValue();
        String sessionSummary = "";
        if (sessionState != null) {
            sessionSummary = sessionState.getSummary(applicationContext, getOwnName());
        }
        return applicationContext.getString(R.string.summary_prefix, getCurrentDate(), sessionSummary);
    }

    public interface Action {
        void run(Fragment fragment);
    }
}
