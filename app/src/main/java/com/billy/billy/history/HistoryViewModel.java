package com.billy.billy.history;

import android.app.Application;

import com.billy.billy.R;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class HistoryViewModel extends AndroidViewModel {
    private MutableLiveData<String> mText;

    public HistoryViewModel(Application application) {
        super(application);

        mText = new MutableLiveData<>();
        mText.setValue(application.getString(R.string.history_coming_soon_title));
    }

    public LiveData<String> getText() {
        return mText;
    }
}