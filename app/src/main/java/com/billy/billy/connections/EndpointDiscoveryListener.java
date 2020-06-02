package com.billy.billy.connections;

import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

@MainThread
public interface EndpointDiscoveryListener {
    void onEndpointFound(@NonNull Endpoint endpoint, @NonNull DiscoveredEndpointInfo info);

    void onEndpointLost(@NonNull Endpoint endpoint);
}
