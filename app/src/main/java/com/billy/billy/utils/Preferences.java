package com.billy.billy.utils;

import java.util.Random;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;

import com.billy.billy.R;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public final class Preferences {
    private Preferences() {

    }


    /**
     * Get a 128-bit string representation of an UUID.
     */
    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static class Connections {
        public static final String USER_ID_SEPARATOR = "$$$";  // todo tomer split this class!
        private static final String FILE_NAME = "connections_preferences_file";
        private static final String USER_NAME_PREFIX_KEY = "user_name_prefix_key";
        private static final String USER_NAME_KEY = "user_name_key";
        private static final @StringRes int[] RANDOM_NAMES = {
                R.string.connections_default_user_name_panda,
                R.string.connections_default_user_name_parrot,
                R.string.connections_default_user_name_lemur
        };

        /**
         * Get a default, random name.
         */
        private static String generateName(@NonNull Context context) {
            Preconditions.checkNotNull(context);

            int randomIndex = new Random().nextInt(RANDOM_NAMES.length);
            return context.getString(RANDOM_NAMES[randomIndex]);
        }

        private static String getUserNamePrefix(@NonNull Context context) {
            Preconditions.checkNotNull(context);

            SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);

            String userId = sharedPreferences.getString(USER_NAME_PREFIX_KEY, null);
            if (userId == null) {
                userId = generateUUID();
                sharedPreferences.edit().putString(USER_NAME_PREFIX_KEY, userId).apply();
            }
            return userId;
        }

        public static String getUserName(@NonNull Context context) {
            Preconditions.checkNotNull(context);

            SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);

            String userName = sharedPreferences.getString(USER_NAME_KEY, null);
            if (userName == null) {
                userName = generateName(context);
                sharedPreferences.edit().putString(USER_NAME_KEY, userName).apply();
            }

            return userName;
        }

        public static String getUserUniqueID(@NonNull Context context) {
            // todo tomer better naming (see endpoint)
            Preconditions.checkNotNull(context);

            return getUserNamePrefix(context) + USER_ID_SEPARATOR + getUserName(context);
        }

        public static void setUserName(@NonNull Context context, @NonNull String userName) {
            Preconditions.checkNotNull(context);
            Preconditions.checkArgument(!Strings.isNullOrEmpty(userName));

            SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(USER_NAME_KEY, userName);
            editor.apply();
        }
    }
}
