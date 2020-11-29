package com.billy.billy.text_recognition;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkNotNull;

@AutoValue
public abstract class BillItem {
    public abstract String name();

    public abstract double price();

    public abstract int amount();

    public abstract double total();

    public static BillItem create(@NonNull String name, double price, int amount, double total) {
        checkNotNull(name);

        return builder()
                .name(name)
                .price(price)
                .amount(amount)
                .total(total)
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

        public abstract Builder total(double total);

        public abstract BillItem build();
    }

    public static DiffUtil.ItemCallback<BillItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<BillItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull BillItem oldItem, @NonNull BillItem newItem) {
            return oldItem.equals(newItem);
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull BillItem oldItem, @NonNull BillItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}
