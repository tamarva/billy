package com.billy.billy.connections;

import java.io.Serializable;

import android.annotation.SuppressLint;

import com.billy.billy.utils.Preferences;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Represents a device we can talk to.
 */
@AutoValue
public abstract class Endpoint implements Serializable {
    public abstract String getId();

    public abstract String getNamePrefix();

    public abstract String getName();

    public abstract String getFullName();

    public static TypeAdapter<Endpoint> typeAdapter(Gson gson) {
        return new AutoValue_Endpoint.GsonTypeAdapter(gson);
    }

    public static Endpoint create(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpointId));
        Preconditions.checkNotNull(discoveredEndpointInfo);

        return create(endpointId, discoveredEndpointInfo.getEndpointName());
    }

    public static Endpoint create(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpointId));
        Preconditions.checkNotNull(connectionInfo);

        return create(endpointId, connectionInfo.getEndpointName());
    }

    public static Endpoint create(@NonNull String endpointId, @NonNull String endpointName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpointId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpointName));

        String[] namePrefix = endpointName.split(Preferences.Connections.USER_ID_SEPARATOR);
        Preconditions.checkState(namePrefix.length == 2);

        return create(endpointId, namePrefix[0], namePrefix[1], endpointName);
    }

    public static Endpoint create(@NonNull String endpointId, @NonNull String namePrefix, @NonNull String name,
                                  @NonNull String fullName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endpointId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(namePrefix));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fullName));

        return new AutoValue_Endpoint(endpointId, namePrefix, name, fullName);
    }

    public static DiffUtil.ItemCallback<Endpoint> DIFF_CALLBACK = new DiffUtil.ItemCallback<Endpoint>() {
        @Override
        public boolean areItemsTheSame(@NonNull Endpoint oldItem, @NonNull Endpoint newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Endpoint oldItem, @NonNull Endpoint newItem) {
            return oldItem.equals(newItem);
        }
    };
}
