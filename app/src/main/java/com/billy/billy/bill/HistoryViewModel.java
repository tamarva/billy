package com.billy.billy.bill;

import android.app.Application;

import com.billy.billy.R;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistoryViewModel extends ViewModel {
    private MutableLiveData<String> mText;

    public HistoryViewModel(Application application) {
        mText = new MutableLiveData<>();
        mText.setValue(application.getString(R.string.history_coming_soon_title));
    }

    public LiveData<String> getText() {
        return mText;
    }
}