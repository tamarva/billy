package com.billy.billy.sessions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import android.content.Context;
import android.util.Log;

import com.billy.billy.R;
import com.billy.billy.text_recognition.Bill;
import com.billy.billy.text_recognition.BillItem;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import androidx.annotation.NonNull;

/**
 * Contains all the meta-data needed for the session.
 */
@AutoValue
public abstract class SessionState implements Serializable {
    private static final String TAG = "SessionState";

    public abstract String getId();

    public abstract List<String> getParticipants();

    public abstract Bill getBill();

    public abstract List<SessionItem> getSessionItems();

    public static SessionState create(@NonNull List<String> participants, @NonNull Bill bill) {
        checkArgument(participants != null && !participants.isEmpty());
        checkNotNull(bill);

        return create(UUID.randomUUID().toString(), participants, bill);
    }

    public static SessionState create(@NonNull String id, @NonNull List<String> participants, @NonNull Bill bill) {
        checkArgument(!Strings.isNullOrEmpty(id));
        checkArgument(participants != null && !participants.isEmpty());
        checkNotNull(bill);

        return create(id, participants, bill, sessionItemsFromBill(bill));
    }

    private static List<SessionItem> sessionItemsFromBill(@Nonnull Bill bill) {
        List<SessionItem> sessionItems = new ArrayList<>();

        List<BillItem> billItems = bill.billItems();
        billItems.sort((billItem1, billItem2) -> billItem1.name().compareTo(billItem2.name()));

        for (BillItem billItem : billItems) {
            sessionItems.add(SessionItem.create(billItem.name(), billItem.price()));
        }

        return sessionItems;
    }

    public static SessionState create(@NonNull String id, @NonNull List<String> participants, @NonNull Bill bill,
                                      @NonNull List<SessionItem> sessionItems) {
        checkArgument(!Strings.isNullOrEmpty(id));
        checkArgument(participants != null && !participants.isEmpty());
        checkNotNull(bill);
        checkNotNull(sessionItems);

        return builder()
                .setId(id)
                .setParticipants(participants)
                .setBill(bill)
                .setSessionItems(sessionItems)
                .build();
    }

    public SessionState withNewParticipant(@NonNull String newParticipant) {
        checkNotNull(newParticipant);

        ArrayList<String> currParticipants = new ArrayList<>(getParticipants());
        if (currParticipants.contains(newParticipant)) {
            Log.e(TAG, "Got an already added participant.");
            return this;
        }

        currParticipants.add(newParticipant);
        return toBuilder()
                .setParticipants(currParticipants)
                .build();
    }

    public SessionState withRemoveParticipant(@NonNull String newParticipant) {
        checkNotNull(newParticipant);

        ArrayList<String> currParticipants = new ArrayList<>(getParticipants());
        if (!currParticipants.contains(newParticipant)) {
            Log.e(TAG, "Got an unknown participant.");
            return this;
        }

        currParticipants.remove(newParticipant);
        return toBuilder()
                .setParticipants(currParticipants)
                .build();
    }

    public SessionState withSelection(Selection selection) {
        checkArgument(selection.getSelectedSessionItemIndex() >= 0);
        checkArgument(selection.getSelectedSessionItemIndex() < getSessionItems().size());

        if (!getParticipants().contains(selection.getSelectingParticipant())) {
            Log.e(TAG, "Got an unknown participant.");
            return this;
        }

        ArrayList<SessionItem> sessionItems = new ArrayList<>(getSessionItems());
        SessionItem currSessionItem = sessionItems.get(selection.getSelectedSessionItemIndex());
        SessionItem newSessionItem = currSessionItem.withSelection(selection);
        sessionItems.set(selection.getSelectedSessionItemIndex(), newSessionItem);
        return toBuilder()
                .setSessionItems(sessionItems)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_SessionState.Builder();
    }

    public abstract Builder toBuilder();

    public String getSummary(@NonNull Context context, @NonNull String ownName) {
        checkNotNull(context);
        checkArgument(!Strings.isNullOrEmpty(ownName));

        String participantsFormat = context.getString(R.string.participants_format);
        for (String participant : getParticipants()) {
            participantsFormat += participant + "\n";
        }

        String billFormat = context.getString(R.string.bill_format);
        for (BillItem billItem : getBill().billItems()) {
            billFormat += billItem + "\n";
        }

        String youChoseFormat = context.getString(R.string.you_chose_format);
        for (SessionItem sessionItem : getSessionItems()) {
            if (sessionItem.getOrderingParticipants().contains(ownName)) {
                youChoseFormat += sessionItem + "\n";
            }
        }

        return participantsFormat + billFormat + youChoseFormat;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(String id);

        public abstract Builder setParticipants(List<String> Participants);

        public abstract Builder setBill(Bill bill);

        public abstract Builder setSessionItems(List<SessionItem> sessionItems);

        public abstract SessionState build();
    }
}
