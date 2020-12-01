package com.billy.billy.connections;

import android.content.Context;

import com.billy.billy.R;
import com.billy.billy.utils.Preferences;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

@AutoValue
public abstract class ConnectionModel {
    /**
     * This represents the action this connection is for.
     * Only devices using the same strategy and service id will appear when discovering.
     */
    public abstract String serviceId();

    public abstract int apiVersion();

    public abstract Strategy strategy();

    public abstract String name();

    public static TypeAdapter<ConnectionModel> typeAdapter(Gson gson) {
        return new AutoValue_ConnectionModel.GsonTypeAdapter(gson);
    }

    @VisibleForTesting
    public static ConnectionModel create(@NonNull String serviceId, int apiVersion, @NonNull Strategy strategy,
                                         @NonNull String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serviceId));
        Preconditions.checkNotNull(strategy);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        return new AutoValue_ConnectionModel(serviceId, apiVersion, strategy, name);
    }

    public static ConnectionModel createDefault(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        String serviceId = context.getString(R.string.connection_service_id);
        int apiVersion = context.getResources().getInteger(R.integer.connection_api_version);
        Strategy strategy = Strategy.P2P_STAR;
        String name = Preferences.Connections.getUserUniqueID(context);
        return create(serviceId, apiVersion, strategy, name);
    }
}
