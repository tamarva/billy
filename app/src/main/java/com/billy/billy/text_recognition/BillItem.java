package com.billy.billy.text_recognition;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkNotNull;

@AutoValue
public abstract class BillItem {
    public abstract int amount();

    public abstract String name();

    public abstract double price();

    public abstract double total();

    public static BillItem create(int amount, @NonNull String name, double price, double total) {
        checkNotNull(name);

        return builder()
                .amount(amount)
                .name(name)
                .price(price)
                .total(total)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_BillItem.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder amount(int amount);

        public abstract Builder name(String name);

        public abstract Builder price(double price);

        public abstract Builder total(double total);

        public abstract BillItem build();
    }
}
