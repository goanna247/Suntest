package com.work.libtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MeasurementArrayAdapter extends ArrayAdapter<Measurement> {
    private static final String TAG = "MeasurementArrayAdapter";
    private List<Measurement> measurementList = new ArrayList<Measurement>();

    static class MeasurementViewHolder {
        TextView measurementName;
        TextView date;
        TextView time;
        TextView temp;
        TextView nanotesla; //wtf is this
        TextView depth;
        TextView dip;
        TextView roll;
        TextView azimuth;
    }

    public MeasurementArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public void add(Measurement object) {
        measurementList.add(object);
        super.add(object);
    }

    @Override
    public int getCount() {
        return this.measurementList.size();
    }

    @Override
    public Measurement getItem(int index) {
        return this.measurementList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        MeasurementViewHolder viewHolder;
        if (row == null) {
            LayoutInflater inflator = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflator.inflate(R.layout.listview_row_layout, parent, false);
            viewHolder = new MeasurementViewHolder();
            viewHolder.measurementName = (TextView) row.findViewById(R.id.measurement_name);
            viewHolder.date = (TextView) row.findViewById(R.id._date);
            viewHolder.time = (TextView) row.findViewById(R.id._time);
            viewHolder.temp = (TextView) row.findViewById(R.id._temp);
            viewHolder.nanotesla = (TextView) row.findViewById(R.id._nanotesla);
            viewHolder.depth = (TextView) row.findViewById(R.id._depth);
            viewHolder.dip = (TextView) row.findViewById(R.id._dip);
            viewHolder.roll = (TextView) row.findViewById(R.id._roll);
            viewHolder.azimuth = (TextView) row.findViewById(R.id._azimuth);
            row.setTag(viewHolder);
        } else {
            viewHolder = (MeasurementViewHolder) row.getTag();
        }
        Measurement measurement = getItem(position);
        viewHolder.measurementName.setText(measurement.getName());
        viewHolder.date.setText(measurement.getDate());
        viewHolder.time.setText(measurement.getTime());
        viewHolder.temp.setText(measurement.getTemp());
        viewHolder.nanotesla.setText(measurement.getNanotesla());
        viewHolder.depth.setText(measurement.getDepth());
        viewHolder.dip.setText(measurement.getDip());
        viewHolder.roll.setText(measurement.getRoll());
        viewHolder.azimuth.setText(measurement.getAzimuth());
        return row;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
