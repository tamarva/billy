package com.billy.billy.sessions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

@AutoValue
public abstract class SessionItem {
    private static final String TAG = "SessionItem";

    public abstract String getItemName();

    public abstract double getItemPrice();

    public abstract List<String> getOrderingParticipants();

    public boolean doesContainParticipant(@NonNull String participantName) {
        checkArgument(!Strings.isNullOrEmpty(participantName));

        return getOrderingParticipants().contains(participantName);
    }

    public double getPricePerParticipant() {
        return getItemPrice() / getOrderingParticipants().size();
    }

    public static TypeAdapter<SessionItem> typeAdapter(Gson gson) {
        return new AutoValue_SessionItem.GsonTypeAdapter(gson);
    }

    public static SessionItem create(@NonNull String itemName, double itemPrice) {
        checkNotNull(itemName);

        return create(itemName, itemPrice, Collections.emptyList());
    }

    public static SessionItem create(@NonNull String itemName, double itemPrice,
                                     @NonNull List<String> orderingParticipants) {
        checkNotNull(itemName);
        checkNotNull(orderingParticipants);

        return builder()
                .setItemName(itemName)
                .setItemPrice(itemPrice)
                .setOrderingParticipants(orderingParticipants)
                .build();
    }

    public SessionItem withSelection(@NonNull Selection selection) {
        checkNotNull(selection);

        if (selection.getIsSelecting()) {
            return withNewOrderingParticipant(selection.getSelectingParticipant());
        } else {
            return withRemoveOrderingParticipant(selection.getSelectingParticipant());
        }
    }

    public SessionItem withNewOrderingParticipant(@NonNull String newOrderingParticipant) {
        checkNotNull(newOrderingParticipant);

        ArrayList<String> currOrdering = new ArrayList<>(getOrderingParticipants());
        if (currOrdering.contains(newOrderingParticipant)) {
            Log.e(TAG, "Got an already added endpoint.");
            return this;
        }

        currOrdering.add(newOrderingParticipant);
        return toBuilder()
                .setOrderingParticipants(currOrdering)
                .build();
    }

    public SessionItem withRemoveOrderingParticipant(@NonNull String oldOrderingParticipant) {
        checkNotNull(oldOrderingParticipant);

        ArrayList<String> currOrdering = new ArrayList<>(getOrderingParticipants());
        if (!currOrdering.contains(oldOrderingParticipant)) {
            Log.e(TAG, "Got an unknown endpoint.");
            return this;
        }

        currOrdering.remove(oldOrderingParticipant);
        return toBuilder()
                .setOrderingParticipants(currOrdering)
                .build();
    }

    public static DiffUtil.ItemCallback<SessionItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<SessionItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull SessionItem oldItem, @NonNull SessionItem newItem) {
            return oldItem.equals(newItem);
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull SessionItem oldItem, @NonNull SessionItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    public static Builder builder() {
        return new AutoValue_SessionItem.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setItemName(String itemName);

        public abstract Builder setItemPrice(double price);

        public abstract Builder setOrderingParticipants(List<String> orderingParticipants);

        public abstract SessionItem build();
    }
}
