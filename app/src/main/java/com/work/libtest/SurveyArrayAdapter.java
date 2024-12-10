package com.work.libtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class SurveyArrayAdapter extends ArrayAdapter<DetailedMeasurement> {
    private static final String TAG = "Survey Array Adapter";
    private List<DetailedMeasurement> surveyList = new ArrayList<>();

    static class SurveyViewHolder {
        TextView date;
        TextView holeID;
        TextView time;
    }

    public SurveyArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public void add(DetailedMeasurement object) {
        surveyList.add(object);
        super.add(object);
    }

    @Override
    public int getCount() {
        return this.surveyList.size();
    }

    @Override
    public DetailedMeasurement getItem(int index) {
        return this.surveyList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SurveyViewHolder surveyViewHolder;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.listitem_resume_survey, parent, false);
            surveyViewHolder = new SurveyViewHolder();
            surveyViewHolder.date = (TextView) row.findViewById(R.id.survey_date);
            surveyViewHolder.time = (TextView) row.findViewById(R.id.survey_time);
            surveyViewHolder.holeID = (TextView) row.findViewById(R.id.survey_probeID);
            row.setTag(surveyViewHolder);

        } else {
            surveyViewHolder = (SurveyViewHolder) row.getTag();
        }
        DetailedMeasurement measurement = getItem(position);
        surveyViewHolder.date.setText(measurement.getBasicMeasurement().getDate());
        surveyViewHolder.time.setText(measurement.getBasicMeasurement().getTime());
        surveyViewHolder.holeID.setText(measurement.getHoleID());
        return row;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
