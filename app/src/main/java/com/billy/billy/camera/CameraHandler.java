package com.billy.billy.camera;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.billy.billy.home.HomeViewModel;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

public class CameraHandler implements HomeViewModel.Action {
    private static final String TAG = CameraHandler.class.getSimpleName();
    private int requestCode;
    private String imagePath;

    public CameraHandler(int requestCode) {
        this.requestCode = requestCode;
    }

    public String getImagePath() {
        return imagePath;
    }

    private File createImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "BILLY_IMAGE_" + timeStamp;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".png", storageDir);
        imagePath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent(Fragment fragment) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(fragment.getContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(fragment.getContext());
            } catch (IOException ex) {
                Log.e(TAG, "Error while creating a file for the image.");
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(fragment.getContext(),
                        fragment.getContext().getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                fragment.startActivityForResult(takePictureIntent, requestCode);
            }
        }
    }

    @Override
    public void run(Fragment fragment) {
        dispatchTakePictureIntent(fragment);
    }
}
