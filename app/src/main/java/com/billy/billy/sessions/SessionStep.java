package com.billy.billy.sessions;

import java.io.Serializable;

import com.google.auto.value.AutoValue;

/**
 * Information of a single updating step.
 */
@AutoValue
public abstract class SessionStep implements Serializable {
    public SessionStep create() {
        return builder()
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SessionStep.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract SessionStep build();
    }
}
