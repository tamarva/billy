package com.billy.billy.text_recognition;

import java.util.ArrayList;
import java.util.List;

import com.google.auto.value.AutoValue;

import androidx.annotation.NonNull;

@AutoValue
public abstract class Bill {
    public abstract List<BillItem> billItems();

    public boolean isEmpty() {
        return billItems().isEmpty();
    }

    public static Bill createEmpty() {
        return builder().build();
    }

    public static Bill create(@NonNull List<BillItem> billItems) {
        return builder()
                .billItems(billItems)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Bill.Builder()
                .billItems(new ArrayList<>());
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder billItems(List<BillItem> billItems);

        public abstract Bill build();
    }
}
