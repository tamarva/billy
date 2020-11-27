package com.billy.billy.History;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.billy.billy.R;

public class HistoryViewModel extends ViewModel {
    private MutableLiveData<String> mText;

    public HistoryViewModel(Application application) {
        mText = new MutableLiveData<>();
        mText.setValue(application.getString(R.string.coming_soon_title));
    }

    public LiveData<String> getText() {
        return mText;
    }
}