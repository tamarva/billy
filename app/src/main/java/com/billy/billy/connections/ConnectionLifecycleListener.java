package com.billy.billy.connections;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

@MainThread
public interface ConnectionLifecycleListener {
    // TODO: Change connectionIfo to auth token only.
    void onConnectionInitiated(@NonNull Endpoint endpoint, @NonNull ConnectionInfo connectionInfo);

    // TODO: Split to accepted, rejected, error.
    void onConnectionResult(@NonNull Endpoint endpoint, @NonNull ConnectionResolution result);

    void onDisconnected(@NonNull Endpoint endpoint);
}
