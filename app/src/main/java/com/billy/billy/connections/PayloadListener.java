package com.billy.billy.connections;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

@MainThread
public interface PayloadListener {
    void onPayloadReceived(@NonNull Endpoint endpoint, @NonNull Payload payload);

    void onPayloadTransferUpdate(@NonNull Endpoint endpoint, @NonNull PayloadTransferUpdate update);
}
