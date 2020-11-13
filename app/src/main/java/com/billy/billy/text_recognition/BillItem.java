package com.billy.billy.text_recognition;

import com.google.auto.value.AutoValue;

import androidx.annotation.NonNull;

@AutoValue
public abstract class BillItem {
    public abstract String name();

    public abstract double price();

    public abstract int amount();

    public static BillItem create(@NonNull String name, double price, int amount) {
        return builder()
                .name(name)
                .price(price)
                .amount(amount)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_BillItem.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder price(double price);

        public abstract Builder amount(int amount);

        public abstract BillItem build();
    }
}
