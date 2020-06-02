package com.billy.billy.home;

import com.billy.billy.camera.CameraHandler;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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