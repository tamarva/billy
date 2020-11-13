package com.billy.billy.utils;

import android.os.Looper;

import com.google.common.base.Preconditions;

import androidx.annotation.NonNull;

public final class ThreadUtils {
    private ThreadUtils() {

    }

    /**
     * Assert that the caller is running on the main thread.
     */
    public static void assertOnMainThread() {
        Preconditions.checkState(isOnMainThread());
    }

    /**
     * Assert that the caller is running on a background thread.
     */
    public static void assertOnBackgroundThread() {
        Preconditions.checkState(isOnBackgroundThread());
    }

    /**
     * Assert that the caller is running on the specified {@code looper}'s thread.
     */
    public static void assertOnThread(@NonNull Looper looper) {
        Preconditions.checkState(isOnThread(looper));
    }

    /**
     * Returns {@code true} iff the caller is running on the main thread.
     */
    public static boolean isOnMainThread() {
        return isOnThread(Looper.getMainLooper());
    }

    /**
     * Returns {@code true} iff the caller is running on a background thread.
     */
    public static boolean isOnBackgroundThread() {
        return !isOnMainThread();
    }

    /**
     * Returns {@code true} iff the caller is running on the {@code looper}'s thread.
     */
    public static boolean isOnThread(@NonNull Looper looper) {
        Preconditions.checkNotNull(looper);

        return looper.isCurrentThread();
    }
}
