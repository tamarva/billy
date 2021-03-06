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
import com.billy.billy.utils.Preferences;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

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

    public double getPriceForParticipant(@NonNull String participantName) {
        checkArgument(!Strings.isNullOrEmpty(participantName));

        return getSessionItems()
                .stream()
                .filter(sessionItem -> sessionItem.doesContainParticipant(participantName))
                .mapToDouble(SessionItem::getPricePerParticipant)
                .sum();
    }

    public static TypeAdapter<SessionState> typeAdapter(Gson gson) {
        return new AutoValue_SessionState.GsonTypeAdapter(gson);
    }

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
            for (int i = 0; i < billItem.amount(); ++i) {
                sessionItems.add(SessionItem.create(billItem.name(), billItem.price()));
            }
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

    public static String normalizeName(@NonNull String fullName) {
        return fullName.split(Preferences.Connections.USER_ID_SEPARATOR)[1];
    }

    public String getSummary(@NonNull Context context, @NonNull String ownName) {
        checkNotNull(context);
        checkArgument(!Strings.isNullOrEmpty(ownName));

        String participantsFormat = context.getString(R.string.participants_format);
        for (String participant : getParticipants()) {
            if (!participant.equals(ownName)) {
                participantsFormat += normalizeName(participant) + "\n";
            }
        }

        String billFormat = context.getString(R.string.bill_format);
        String billDescriptionFormat = "%s:\t%.1f\tx%s\t%.1f\n";
        for (BillItem billItem : getBill().billItems()) {
            billFormat += String.format(billDescriptionFormat, billItem.name(), billItem.price(), billItem.amount(),
                    billItem.total());
        }

        String youChoseFormat = context.getString(R.string.you_chose_format);
        for (SessionItem sessionItem : getSessionItems()) {
            if (sessionItem.doesContainParticipant(ownName)) {
                String curr = sessionItem.getItemName();
                if (sessionItem.getOrderingParticipants().size() > 1) {
                    curr += "\t(+";
                    for (String participant : sessionItem.getOrderingParticipants()) {
                        if (!participant.equals(ownName)) {
                            curr += normalizeName(participant) + "\n";
                        }
                    }
                    curr = curr.trim() + ")";
                }
                youChoseFormat += curr + "\n";
            }
        }

        return participantsFormat + "\n" + billFormat + "\n" + youChoseFormat;
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
