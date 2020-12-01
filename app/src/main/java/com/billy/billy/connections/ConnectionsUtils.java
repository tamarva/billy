package com.billy.billy.connections;

import java.util.Locale;

import android.Manifest;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public final class ConnectionsUtils {
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };
    /**
     * A set of background colors.
     * We'll hash the authentication token we get from connecting to a device to pick a color randomly from this list.
     * Devices with the same background color are talking to each other securely
     * (with 1/COLORS.length chance of collision with another pair of devices).
     */
    @ColorInt private static final int[] COLORS = new int[]{
            0xFFF44336 /* red */,
            0xFF9C27B0 /* deep purple */,
            0xFF00BCD4 /* teal */,
            0xFF4CAF50 /* green */,
            0xFFFFAB00 /* amber */,
            0xFFFF9800 /* orange */,
            0xFF795548 /* brown */
    };

    private ConnectionsUtils() {

    }

    /**
     * These permissions are required before connecting to Nearby Connections. Only {@link Manifest.permission#ACCESS_COARSE_LOCATION} is
     * considered dangerous, so the others should be granted just by having them in the manifest.
     */
    public static String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    /**
     * Transforms a {@link Status} into a English-readable message for logging, eg. [404]File not found.
     */
    static String getDebugMessageFromStatus(@NonNull Status status) {
        Preconditions.checkNotNull(status);

        String statusMessage = status.getStatusMessage() != null
                ? status.getStatusMessage()
                : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode());
        return String.format(Locale.ENGLISH, "[%d]%s", status.getStatusCode(), statusMessage);
    }

    /**
     * The background color of the 'CONNECTED' state.
     * This is randomly chosen from the {@link #COLORS} list, based off the authentication token.
     * We'll use the auth token, which is the same on both devices, to pick a color to use when we're connected.
     * This way, users can visually see which device they connected with.
     */
    @ColorInt
    static int getColorForConnection(@NonNull String authenticationToken) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(authenticationToken));

        return COLORS[authenticationToken.hashCode() % COLORS.length];
    }
}
