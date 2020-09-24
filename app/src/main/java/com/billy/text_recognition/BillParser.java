package com.billy.text_recognition;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.ml.vision.text.FirebaseVisionText;

public interface BillParser {
    @NonNull Bill parse(@NonNull Context context, @NonNull FirebaseVisionText firebaseVisionText);
}
