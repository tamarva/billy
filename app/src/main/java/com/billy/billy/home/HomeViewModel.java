package com.billy.billy.home;

import android.app.Application;
import android.util.Log;

import com.billy.billy.camera.CameraHandler;
import com.billy.text_recognition.TextRecognition;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class HomeViewModel extends AndroidViewModel {
    private static final String TAG = HomeViewModel.class.getSimpleName();
    static final int REQUEST_TAKE_PHOTO = 1;

    public interface Action {
        void run(Fragment fragment);
    }

    private MutableLiveData<Action> action;
    private CameraHandler cameraHandler;
    private boolean canHandleClick = true;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        cameraHandler = new CameraHandler(REQUEST_TAKE_PHOTO);
        action = new MutableLiveData<>();
    }

    public LiveData<Action> getAction() {
        return action;
    }

    public void onCameraButtonClicked() {
        if (canHandleClick) {
            canHandleClick = false;
            action.setValue(cameraHandler);
        }
    }

    public void onBillScanned() {
        canHandleClick = true;
        TextRecognition textRecognition = new TextRecognition();
        textRecognition.detectText(getApplication(), cameraHandler.getImageUri(),
                bill -> Log.d(TAG, "Got result: " + bill.toString()));
    }

    public void onCancel() {
        canHandleClick = true;
    }
}