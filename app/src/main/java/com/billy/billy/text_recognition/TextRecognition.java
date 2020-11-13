package com.billy.billy.text_recognition;

import java.io.IOException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.billy.billy.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import androidx.annotation.NonNull;

/**
 * This class is responsible for getting an image and performing OCR on it.
 */
public class TextRecognition {
    private static final String TAG = TextRecognition.class.getSimpleName();

    public interface TextRecognitionResultsListener {
        void onResult(@NonNull Bill bill);
    }

    private final BillParser billParser;

    public TextRecognition() {
        this(new BillParserImpl());
    }

    public TextRecognition(@NonNull BillParser billParser) {
        this.billParser = billParser;
    }

    public void detectText(@NonNull Context context, @NonNull Uri imageFile,
                           @NonNull TextRecognitionResultsListener textRecognitionResultsListener) {
        FirebaseApp.initializeApp(context);

        FirebaseVisionImage firebaseVisionImage;
        try {
            firebaseVisionImage = FirebaseVisionImage.fromFilePath(context, imageFile);
        } catch (IOException ioException) {
            Toast.makeText(context, R.string.ask_user_to_retry, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error: ", ioException);
            return;
        }

        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        firebaseVisionTextRecognizer.processImage(firebaseVisionImage)
                .addOnSuccessListener(firebaseVisionText -> {
                    Bill bill = billParser.parse(context, firebaseVisionText);
                    textRecognitionResultsListener.onResult(bill);
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(context, R.string.ask_user_to_retry, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error: ", error);
                });
    }
}
