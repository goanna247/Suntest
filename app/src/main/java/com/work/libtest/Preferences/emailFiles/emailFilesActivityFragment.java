package com.work.libtest.Preferences.emailFiles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.work.libtest.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class emailFilesActivityFragment extends Fragment {

    public emailFilesActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_files, container, false);
    }
}
