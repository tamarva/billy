package com.billy.billy.connections;

import static com.billy.billy.connections.ConnectionState.CONNECTED;
import static com.billy.billy.connections.ConnectionState.CONNECTING;
import static com.billy.billy.connections.ConnectionState.IDLE;
import static com.billy.billy.connections.ConnectionsUtils.getDebugMessageFromStatus;
import static com.billy.billy.utils.ThreadUtils.assertOnMainThread;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

@MainThread
public class ConnectionsService {
    private static final String TAG = ConnectionsService.class.getSimpleName();
    private final ConnectionsClient connectionsClient;
    private final ConnectionModel connectionModel;
    private final ConnectionLifecycleListener connectionLifecycleListener;
    private final EndpointDiscoveryListener endpointDiscoveryListener;
    private final PayloadListener payloadListener;
    private final Handler handler;
    private final ConnectionLifecycleCallback connectionLifecycleCallback;
    private final EndpointDiscoveryCallback endpointDiscoveryCallback;
    private final PayloadCallback payloadCallback;
    /**
     * The devices we've discovered near us.
     */
    private final Map<String, Endpoint> discoveredEndpoints = new HashMap<>();
    /**
     * The devices we have pending connections to.
     * They will stay pending until we call {@link #acceptConnection} or {@link #rejectConnection}.
     */
    private final Map<String, Endpoint> pendingConnections = new HashMap<>();
    /**
     * The devices we are currently connected to. For advertisers, this may be large.
     * For discoverers, there will only be one entry in this map.
     */
    private final Map<String, Endpoint> establishedConnections = new HashMap<>();
    private ConnectionState connectionState = IDLE;  // Only relevant for discoverers.
    private ConnectionRole connectionRole = ConnectionRole.UNKNOWN;

    public ConnectionsService(@NonNull Context applicationContext,
                              @NonNull ConnectionLifecycleListener connectionLifecycleListener,
                              @NonNull EndpointDiscoveryListener endpointDiscoveryListener,
                              @NonNull PayloadListener payloadListener) {
        this(Nearby.getConnectionsClient(applicationContext), ConnectionModel.createDefault(applicationContext),
                connectionLifecycleListener, endpointDiscoveryListener, payloadListener);
    }

    @VisibleForTesting
    public ConnectionsService(@NonNull ConnectionsClient connectionsClient,
                              @NonNull ConnectionModel connectionModel,
                              @NonNull ConnectionLifecycleListener connectionLifecycleListener,
                              @NonNull EndpointDiscoveryListener endpointDiscoveryListener,
                              @NonNull PayloadListener payloadListener) {
        this.connectionsClient = checkNotNull(connectionsClient);
        this.connectionModel = checkNotNull(connectionModel);
        this.connectionLifecycleListener = checkNotNull(connectionLifecycleListener);
        this.endpointDiscoveryListener = checkNotNull(endpointDiscoveryListener);
        this.payloadListener = checkNotNull(payloadListener);

        handler = new Handler(Looper.getMainLooper());
        connectionLifecycleCallback = createConnectionLifecycleCallback();
        endpointDiscoveryCallback = createEndpointDiscoveryCallback();
        payloadCallback = createPayPayloadCallback();
    }

    private ConnectionLifecycleCallback createConnectionLifecycleCallback() {
        return new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                handler.post(() -> onConnectionInitiatedCallback(endpointId, connectionInfo));
            }

            @Override
            public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
                handler.post(() -> onConnectionResultCallback(endpointId, result));
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                handler.post(() -> onDisconnectedCallback(endpointId));
            }
        };
    }

    private void onConnectionInitiatedCallback(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        Log.d(TAG, String.format(Locale.ENGLISH,
                "Connection Initiated. endpointId=%s, endpointName=%s.", endpointId, connectionInfo.getEndpointName()));

        Endpoint endpoint = null;
        switch (connectionRole) {
            case ADVERTISER:
                endpoint = Endpoint.create(endpointId, connectionInfo.getEndpointName());
                break;
            case DISCOVERER:
                checkState(connectionState == CONNECTING);
                endpoint = discoveredEndpoints.get(endpointId);
                checkState(endpoint != null);
                break;
            case UNKNOWN:
                throw new IllegalStateException("Can't initiate connection while having UNKNOWN role.");
        }

        Endpoint previousEndpoint = pendingConnections.put(endpointId, endpoint);
        checkState(previousEndpoint == null);

        connectionLifecycleListener.onConnectionInitiated(endpoint, connectionInfo);
    }

    private void onConnectionResultCallback(@NonNull String endpointId, @NonNull ConnectionResolution result) {
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTING);

        Endpoint endpoint = pendingConnections.remove(endpointId);
        checkState(endpoint != null);

        if (result.getStatus().getStatusCode() == ConnectionsStatusCodes.STATUS_OK) {
            Log.d(TAG, String.format(Locale.ENGLISH, "Connected to endpoint %s.", endpoint.toString()));
            setConnectionState(CONNECTED);
            establishedConnections.put(endpointId, endpoint);
        } else {
            Log.d(TAG, String.format(Locale.ENGLISH, "Couldn't connect to endpoint %s. Received status %s.",
                    endpoint.toString(), getDebugMessageFromStatus(result.getStatus())));
            setConnectionState(IDLE);
        }

        connectionLifecycleListener.onConnectionResult(endpoint, result);
    }

    private void onDisconnectedCallback(@NonNull String endpointId) {
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTED);

        Endpoint endpoint = establishedConnections.remove(endpointId);
        checkState(endpoint != null);

        if (establishedConnections.isEmpty()) {
            setConnectionState(IDLE);  // todo i made a little mess with this, fix
        }

        Log.d(TAG, String.format(Locale.ENGLISH, "Disconnected from endpoint %s.", endpoint));
        connectionLifecycleListener.onDisconnected(endpoint);
    }

    private EndpointDiscoveryCallback createEndpointDiscoveryCallback() {
        return new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                handler.post(() -> onEndpointFoundCallback(endpointId, info));
            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                handler.post(() -> onEndpointLostCallback(endpointId));
            }
        };
    }

    private void onEndpointFoundCallback(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
        Log.d(TAG, String.format(Locale.ENGLISH,
                "Discovered an advertiser: endpointId=%s, endpointName=%s, serviceId=%s.",
                endpointId, info.getEndpointName(), info.getServiceId()));

        if (!connectionModel.serviceId().equals(info.getServiceId())) {
            Log.v(TAG, String.format(Locale.ENGLISH,
                    "The found endpoint has serviceID of %s, rather than %s.",
                    info.getServiceId(), connectionModel.serviceId()));
            return;
        }

        Endpoint endpoint = Endpoint.create(endpointId, info.getEndpointName());
        Endpoint previousEndpoint = discoveredEndpoints.put(endpointId, endpoint);
        checkState(previousEndpoint == null);

        endpointDiscoveryListener.onEndpointFound(endpoint, info);
    }

    private void onEndpointLostCallback(@NonNull String endpointId) {
        Endpoint discoveredEndpoint = discoveredEndpoints.remove(endpointId);
        checkState(discoveredEndpoint != null);
        Log.d(TAG, "Lost endpoint: " + discoveredEndpoint.toString());
        endpointDiscoveryListener.onEndpointLost(discoveredEndpoint);
    }

    private PayloadCallback createPayPayloadCallback() {
        return new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                handler.post(() -> onPayloadReceivedCallback(endpointId, payload));
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                handler.post(() -> onPayloadTransferUpdateCallback(endpointId, update));
            }
        };
    }

    private void onPayloadReceivedCallback(@NonNull String endpointId, @NonNull Payload payload) {
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTED);

        Log.d(TAG, String.format(Locale.ENGLISH,
                "Received payload from endpointId: %s. The payload is:\n%s", endpointId, payload.toString()));

        Endpoint endpoint = establishedConnections.get(endpointId);
        checkState(endpoint != null);
        payloadListener.onPayloadReceived(endpoint, payload);
    }

    private void onPayloadTransferUpdateCallback(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTED);

        Log.d(TAG, String.format(Locale.ENGLISH,
                "Received payload transfer update from endpointId: %s. The update is:\n%s", endpointId, update.toString()));

        Endpoint endpoint = establishedConnections.get(endpointId);
        checkState(endpoint != null);
        payloadListener.onPayloadTransferUpdate(endpoint, update);
    }

    private void setConnectionStateAsync(@NonNull ConnectionState newConnectionState) {
        handler.post(() -> setConnectionState(newConnectionState));
    }

    private void setConnectionState(@NonNull ConnectionState newConnectionState) {
        Log.d(TAG, "Setting ConnectionState to: " + newConnectionState.name());
        if (newConnectionState == connectionState) {
            Log.w(TAG, "connectionState is already " + connectionState.name() + ". Doing nothing");
            return;
        }

        connectionState = newConnectionState;

        if (connectionRole == ConnectionRole.DISCOVERER) {
            if (connectionState == IDLE) {
                startDiscovering();
            } else {
                stopDiscovering();
            }
        }
    }

    public void setConnectionRole(@NonNull ConnectionRole newConnectionRole) {
        checkNotNull(newConnectionRole);
        Log.d(TAG, "Setting ConnectionRole to: " + newConnectionRole.name());
        if (newConnectionRole == connectionRole) {
            Log.w(TAG, "ConnectionRole is already " + connectionRole.name() + ". Doing nothing.");
            return;
        }

        reset();
        connectionRole = newConnectionRole;
        if (connectionRole == ConnectionRole.DISCOVERER) {
            startDiscovering();
        } else if (connectionRole == ConnectionRole.ADVERTISER) {
            startAdvertising();
        }
    }

    private void startAdvertising() {
        Log.d(TAG, "Starting to advertise.");
        checkState(connectionRole == ConnectionRole.ADVERTISER);

        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(connectionModel.strategy())
                .build();
        connectionsClient.startAdvertising(connectionModel.name(), connectionModel.serviceId(),
                connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(ignored -> Log.d(TAG, "Successfully started advertising."))
                .addOnFailureListener(error -> Log.e(TAG, "Failed to start advertising.", error));
    }

    private void stopAdvertising() {
        Log.d(TAG, "Stopping advertise.");
        checkState(connectionRole == ConnectionRole.ADVERTISER);
        connectionsClient.stopAdvertising();
    }

    private void startDiscovering() {
        Log.d(TAG, "Starting discovery.");
        checkState(connectionRole == ConnectionRole.DISCOVERER);

        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder()
                .setStrategy(connectionModel.strategy())
                .build();
        connectionsClient.startDiscovery(connectionModel.serviceId(), endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(ignored -> Log.d(TAG, "Successfully started discovering."))
                .addOnFailureListener(error -> Log.e(TAG, "Failed to start discovering.", error));
    }

    private void stopDiscovering() {
        Log.d(TAG, "Stopping discovery.");
        checkState(connectionRole == ConnectionRole.DISCOVERER);
        connectionsClient.stopDiscovery();
    }

    public void requestConnectionToDiscoveredEndpoint(@NonNull Endpoint endpoint) {
        assertOnMainThread();
        checkNotNull(endpoint);
        checkState(connectionRole == ConnectionRole.DISCOVERER);
        Log.d(TAG, "Requesting connection to endpoint " + endpoint.toString());

        if (connectionState != IDLE) {
            Log.d(TAG, "Requesting connection while not in idle state. Doing nothing.");
            return;
        }
        setConnectionState(CONNECTING);

        connectionsClient.requestConnection(connectionModel.name(), endpoint.getId(), connectionLifecycleCallback)
                .addOnSuccessListener(ignored -> Log.d(TAG, "Successfully requested connection with endpoint: " + endpoint.toString()))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to request connection with endpoint: " + endpoint.toString(), error);
                    setConnectionStateAsync(IDLE);
                });
    }

    public void acceptConnection(@NonNull Endpoint endpoint) {
        assertOnMainThread();
        checkNotNull(endpoint);
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTING);
        checkState(pendingConnections.containsKey(endpoint.getId()));
        Log.d(TAG, "Accepting connection to endpoint: " + endpoint.toString());

        connectionsClient.acceptConnection(endpoint.getId(), payloadCallback)
                .addOnSuccessListener(ignored ->
                        Log.d(TAG, "Successfully accepted connection to endpoint: " + endpoint.toString()))
                .addOnFailureListener(error ->
                        Log.e(TAG, "Failed to accept connection to endpoint: " + endpoint.toString(), error));
    }

    public void rejectConnection(@NonNull Endpoint endpoint) {
        assertOnMainThread();
        checkNotNull(endpoint);
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTING);
        checkState(pendingConnections.containsKey(endpoint.getId()));
        Log.d(TAG, "Rejecting connection from endpoint: " + endpoint.toString());

        connectionsClient.rejectConnection(endpoint.getId())
                .addOnSuccessListener(ignored -> Log.d(TAG, "Successfully rejected connection to endpoint: " + endpoint.toString()))
                .addOnFailureListener(error -> Log.e(TAG, "Failed to reject connection to endpoint: " + endpoint.toString(), error));
    }

    public void disconnectFromEndpoint(@NonNull Endpoint endpoint) {
        assertOnMainThread();
        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTED);
        checkNotNull(endpoint);
        checkState(establishedConnections.containsKey(endpoint.getId()));

        Log.d(TAG, "Disconnecting from endpoint: " + endpoint.toString());
        connectionsClient.disconnectFromEndpoint(endpoint.getId());
    }

    /**
     * Resets and clears all state in Nearby Connections.
     */
    public void reset() {
        assertOnMainThread();
        Log.d(TAG, "Resetting to fresh state.");

        connectionsClient.stopAllEndpoints();
        discoveredEndpoints.clear();
        pendingConnections.clear();
        establishedConnections.clear();

        connectionState = IDLE;

        if (connectionRole == ConnectionRole.DISCOVERER) {
            stopDiscovering();
        } else if (connectionRole == ConnectionRole.ADVERTISER) {
            stopAdvertising();
        }
        connectionRole = ConnectionRole.UNKNOWN;
    }

    public void sendString(@NonNull String payload) {
        sendString(payload, new HashSet<>(establishedConnections.values()));
    }

    public void sendString(@NonNull String payload, @NonNull Set<Endpoint> endpoints) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(payload));

        sendBytes(payload.getBytes(), endpoints);
    }

    public void sendBytes(@NonNull byte[] payload) {
        sendBytes(payload, new HashSet<>(establishedConnections.values()));
    }

    public void sendBytes(@NonNull byte[] payload, @NonNull Set<Endpoint> endpoints) {
        Preconditions.checkNotNull(payload);
        Preconditions.checkArgument(payload.length > 0);

        send(Payload.fromBytes(payload), endpoints);
    }

    public void sendFile(@NonNull Uri path) throws FileNotFoundException {
        sendFile(path, new HashSet<>(establishedConnections.values()));
    }

    public void sendFile(@NonNull Uri path, @NonNull Set<Endpoint> endpoints) throws FileNotFoundException {
        Preconditions.checkNotNull(path);

        sendFile(path.getPath(), endpoints);
    }

    public void sendFile(@NonNull String path) throws FileNotFoundException {
        sendFile(path, new HashSet<>(establishedConnections.values()));
    }

    public void sendFile(@NonNull String path, @NonNull Set<Endpoint> endpoints) throws FileNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path));

        sendFile(new File(path), endpoints);
    }

    public void sendFile(@NonNull File payload) throws FileNotFoundException {
        sendFile(payload, new HashSet<>(establishedConnections.values()));
    }

    public void sendFile(@NonNull File payload, @NonNull Set<Endpoint> endpoints) throws FileNotFoundException {
        Preconditions.checkNotNull(payload);

        send(Payload.fromFile(payload), endpoints);
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     * @param payload The data you want to send.
     */
    public void send(@NonNull Payload payload) {
        send(payload, new HashSet<>(establishedConnections.values()));
    }

    public void send(@NonNull Payload payload, @NonNull Set<Endpoint> endpoints) {
        assertOnMainThread();
        checkNotNull(payload);
        checkNotNull(endpoints);

        if (endpoints.isEmpty()) {
            return;
        }

        checkState(connectionRole == ConnectionRole.ADVERTISER || connectionState == CONNECTED);
        Log.d(TAG, String.format(Locale.ENGLISH, "Sending payload:\n%s\n\nto endpoints:\n%s.", payload, endpoints));

        List<String> sendTo = endpoints.stream()
                .map(Endpoint::getId)
                .collect(Collectors.toList());

        connectionsClient.sendPayload(sendTo, payload)
                .addOnSuccessListener(ignored -> Log.d(TAG, String.format(Locale.ENGLISH,
                        "Successfully sent payload:\n%s\n\nto endpoints:\n%s.", payload, endpoints)))
                .addOnFailureListener(error -> Log.e(TAG,  String.format(Locale.ENGLISH,
                        "Failed to send payload:\n%s\n\nto endpoints:\n%s.", payload, endpoints),
                        error));
    }
}
