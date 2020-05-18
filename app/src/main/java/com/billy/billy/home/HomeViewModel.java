package com.billy.billy.home;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.v4.app.Fragment;

import com.billy.billy.camera.CameraHandler;

public class HomeViewModel extends ViewModel {
    public interface Action {
        void run(Fragment fragment);
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    private MutableLiveData<String> mText;
    private MutableLiveData<Action> action;
    private CameraHandler cameraHandler;
    private boolean canHandleClick = true;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
        cameraHandler = new CameraHandler(REQUEST_TAKE_PHOTO);
        action = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
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

    public void onResults() {
        canHandleClick = true;
        String path = cameraHandler.getImagePath();
    }

    public void onCancel() {
        canHandleClick = true;
    }
}