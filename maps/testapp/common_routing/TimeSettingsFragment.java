package com.yandex.maps.testapp.common_routing;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.maps.testapp.R;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.TimeZone;

public class TimeSettingsFragment extends Fragment {
    private View routeTimePanel;
    private EditText routeTimeEdit;
    private DatePicker routeDatePicker;
    private TimePicker routeTimePicker;

    public interface Listener {
        void onTimeSettingsChanged(@NotNull TimeOptions options);
    };

    @Override
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState)
    {
        View view = inflater.inflate(
            R.layout.fragment_masstransit_time_settings,
            container,
            false);

        routeTimePanel = view.findViewById(R.id.mtroute_set_route_time_panel);
        routeTimeEdit = view.findViewById(R.id.mtroute_time_timestamp_edit);
        routeDatePicker = view.findViewById(R.id.mtroute_date_picker);
        routeTimePicker = view.findViewById(R.id.mtroute_time_picker);
        routeTimePicker.setIs24HourView(true);
        initRouteTimeSelectionControls();

        view.findViewById(R.id.mtroute_apply).setOnClickListener(dummy -> {
            CompoundButton departureButton =
                view.findViewById(R.id.mtroute_set_departure_time_option);
            Long timeValue = Long.parseLong(
                routeTimeEdit.getText().toString()) * 1000;
            if (departureButton.isChecked()) {
                setRouteTime(timeValue, null);
            } else {
                setRouteTime(null, timeValue);
            }
        });

        view.findViewById(R.id.mtroute_apply_as_none).setOnClickListener(
            dummy -> setRouteTime(null, null));

        view.findViewById(R.id.mtroute_cancel).setOnClickListener(
            dummy -> routeTimePanel.setVisibility(View.GONE));

        view.findViewById(R.id.mtroute_reset_to_current).setOnClickListener(
            dummy -> initRouteTimeSelectionControls());

        return view;
    }

    public void show() {
        routeTimePanel.setVisibility(View.VISIBLE);
    }

    private void setRouteTime(Long newDepartureTime, Long newArrivalTime) {
        routeTimePanel.setVisibility(View.GONE);
        Listener activity = (Listener)getActivity();
        activity.onTimeSettingsChanged(
            new TimeOptions(newDepartureTime, newArrivalTime));
    }

    private void initRouteTimeSelectionControls() {
        final Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        routeDatePicker.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH), (a, b, c, d) -> updateSelectedRouteTime());
        routeTimePicker.setCurrentHour(today.get(Calendar.HOUR_OF_DAY));
        routeTimePicker.setCurrentMinute(today.get(Calendar.MINUTE));
        routeTimePicker.setOnTimeChangedListener((a, b, c) -> updateSelectedRouteTime());
        updateSelectedRouteTime();
    }

    private void updateSelectedRouteTime() {
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.set(routeDatePicker.getYear(), routeDatePicker.getMonth(),
                routeDatePicker.getDayOfMonth(), routeTimePicker.getCurrentHour(),
                routeTimePicker.getCurrentMinute(), 0);
        Long timestamp = calendar.getTimeInMillis() / 1000;
        routeTimeEdit.setText(timestamp.toString());
    }
}
