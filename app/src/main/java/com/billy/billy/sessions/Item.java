package com.billy.billy.sessions;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Item {
    public static Item create() {
        return builder()
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Item.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Item build();
    }
}
