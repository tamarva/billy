package com.billy.billy.sessions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class Selection {
    public abstract String getSelectingParticipant();

    public abstract int getSelectedSessionItemIndex();

    /**
     * {@code true} if the user is toggling the selection on, {@code false} is they're toggling it off.
     */
    public abstract boolean getIsSelecting();

    public static TypeAdapter<Selection> typeAdapter(Gson gson) {
        return new AutoValue_Selection.GsonTypeAdapter(gson);
    }

    public static Selection create(@Nonnull String selectingParticipant, int selectedSessionItemIndex,
                                   boolean isSelecting) {
        checkNotNull(selectingParticipant);
        checkArgument(selectedSessionItemIndex >= 0);

        return builder()
                .setSelectingParticipant(selectingParticipant)
                .setSelectedSessionItemIndex(selectedSessionItemIndex)
                .setIsSelecting(isSelecting)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Selection.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setSelectingParticipant(String selectingParticipant);

        public abstract Builder setSelectedSessionItemIndex(int selectedSessionItemIndex);

        public abstract Builder setIsSelecting(boolean isSelecting);

        public abstract Selection build();
    }
}
