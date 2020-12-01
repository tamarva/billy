package com.billy.billy.text_recognition;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import android.annotation.SuppressLint;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

@AutoValue
public abstract class BillItem implements Serializable {
    public abstract String name();

    public abstract double price();

    public abstract int amount();

    public abstract double total();

    public static TypeAdapter<BillItem> typeAdapter(Gson gson) {
        return new AutoValue_BillItem.GsonTypeAdapter(gson);
    }

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
