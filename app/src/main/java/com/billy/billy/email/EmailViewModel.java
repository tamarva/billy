package com.billy.billy.email;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EmailViewModel extends ViewModel {
    private MutableLiveData<Boolean> sendEmail;
    private Application applicationContext;

    public EmailViewModel(Application application) {
        applicationContext = application;
        sendEmail = new MutableLiveData<>();

    }

    public LiveData<Boolean> getText() {
        return sendEmail;
    }
}