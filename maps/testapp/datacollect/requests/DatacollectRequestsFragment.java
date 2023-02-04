package com.yandex.maps.testapp.datacollect.requests;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yandex.maps.testapp.R;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatacollectRequestsFragment extends Fragment implements LastRequestController.Listener {
    private LastRequestController lastRequestController;

    private TextView timestamp;
    private TextView lang;
    private TextView miid;
    private TextView vehicleType;
    private TextView source;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View panel = inflater.inflate(R.layout.datacollect_requests_fragment, container, false);
        timestamp = panel.findViewById(R.id.datacollect_last_request_timestamp);
        lang = panel.findViewById(R.id.datacollect_lang_param);
        miid = panel.findViewById(R.id.datacollect_miid_param);
        vehicleType = panel.findViewById(R.id.datacollect_vehicle_type_param);
        source = panel.findViewById(R.id.datacollect_source_param);

        lastRequestController = new LastRequestController();
        lastRequestController.subscribe(this);

        return panel;
    }

    @Override
    public void onStop() {
        super.onStop();
        lastRequestController.unsubscribe();
    }

    @Override
    public void onLastRequestUpdated(@NonNull LastRequestController.Request request) {
        updateLastRequest(request);
    }

    private void updateLastRequest(LastRequestController.Request request) {
        if (request == null) {
            return;
        }

        timestamp.setText(timestampToString(request.timestamp));
        lang.setText(request.lang == null ? "None" : request.lang);
        miid.setText(request.miid == null ? "None" : request.miid);
        vehicleType.setText(request.vehicleType == null ? "None" : request.vehicleType);
        source.setText(request.source == null ? "None" : request.source);
    }

    static private String timestampToString(long timestamp) {
        final String pattern = "yyyy-MM-dd HH:mm:ss.SSS ";
        Date date = new Date(timestamp);
        return new SimpleDateFormat(pattern, Locale.US).format(date);
    }
}
