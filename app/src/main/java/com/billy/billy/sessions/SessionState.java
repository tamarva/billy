package com.billy.billy.sessions;

import java.io.Serializable;

import com.google.auto.value.AutoValue;

/**
 * Contains all the meta-data needed for the session.
 */
@AutoValue
public abstract class SessionState implements Serializable {
    public abstract String getId();

//    public abstract Uri getBillImageUri();
//
//    public abstract List<Item> getAllItems();
//
//    public abstract String getRestaurantName();
//
//    public abstract Date getCreationDate();
//
//    public abstract List<SessionStep> getSessionSteps();

//    @Nullable
//    public SessionStep getLatSessionSteps() {
//        List<SessionStep> sessionSteps = getSessionSteps();
//        if (sessionSteps.isEmpty()) {
//            return null;
//        }
//        return sessionSteps.get(sessionSteps.size() - 1);
//    }

//    public abstract List<Endpoint> getParticipants();

//    public abstract List<Item> getItems();
//
//    public abstract int getCurrentSessionStepIndex();
//
//    public abstract String getVersion();
//
//    public abstract String getServiceId();
//
//    public abstract Strategy getStrategy();

    //    public SessionStep getCurrentSessionStep() {
//        return getSessionSteps().get(getCurrentSessionStepIndex());
//    }

    public SessionState create() {
        return builder()
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SessionState.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(String id);

        public abstract SessionState build();
    }
}
