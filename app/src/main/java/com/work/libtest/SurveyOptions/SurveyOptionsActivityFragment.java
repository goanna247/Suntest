package com.work.libtest.SurveyOptions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.work.libtest.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SurveyOptionsActivityFragment extends Fragment {

    public SurveyOptionsActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_survey_options, container, false);
    }
}
