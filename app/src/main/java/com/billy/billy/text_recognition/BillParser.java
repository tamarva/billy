package com.billy.billy.text_recognition;

import android.content.Context;

import com.google.firebase.ml.vision.text.FirebaseVisionText;

import androidx.annotation.NonNull;

public interface BillParser {
    @NonNull Bill parse(@NonNull Context context, @NonNull FirebaseVisionText firebaseVisionText);
}
