package com.work.libtest.Preferences.deleteFiles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.work.libtest.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class deleteFilesActivityFragment extends Fragment {

    public deleteFilesActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delete_files, container, false);
    }
}
