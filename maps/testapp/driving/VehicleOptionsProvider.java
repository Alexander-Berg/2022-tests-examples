package com.yandex.maps.testapp.driving;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.yandex.maps.testapp.R;

import com.yandex.mapkit.directions.driving.VehicleType;
import com.yandex.mapkit.directions.driving.VehicleOptions;

import java.util.HashMap;

public class VehicleOptionsProvider {

    public interface Listener {
        void onVehicleOptionsSelected(VehicleOptions vehicleOptions);
    }

    private static final HashMap<VehicleType, String> vehicleTypeNames;
    static {
        vehicleTypeNames = new HashMap<VehicleType, String>();
        vehicleTypeNames.put(VehicleType.DEFAULT, "Car");
        vehicleTypeNames.put(VehicleType.TAXI, "Taxi");
        vehicleTypeNames.put(VehicleType.TRUCK, "Truck");
    }

    static public String nameOf(VehicleType vehicleType) {
        return vehicleTypeNames.get(vehicleType);
    }

    static public void poll(Context ctx, VehicleOptions currentVehicleOptions, Listener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        LayoutInflater inflater = LayoutInflater.from(ctx);

        builder.setView(inflater.inflate(R.layout.driving_vehicle_options, null))
            .setPositiveButton(
                    R.string.driving_apply_vehicle_options,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    VehicleOptions vehicleOptions = new VehicleOptions();

                    AlertDialog dialog = (AlertDialog) dialogInterface;

                    RadioGroup vehicleTypeRadioGroup = (RadioGroup) dialog.findViewById(R.id.set_vehicle_type);
                    int vehicleTypeId = vehicleTypeRadioGroup.getCheckedRadioButtonId();
                    if (vehicleTypeId == R.id.set_vehicle_type_default) {
                        vehicleOptions.setVehicleType(VehicleType.DEFAULT);
                    } else if (vehicleTypeId == R.id.set_vehicle_type_taxi) {
                        vehicleOptions.setVehicleType(VehicleType.TAXI);
                    } else if (vehicleTypeId == R.id.set_vehicle_type_truck) {
                        vehicleOptions.setVehicleType(VehicleType.TRUCK);
                    }

                    EditText weightEdit = (EditText) dialog.findViewById(R.id.set_weight);
                    String weightString = weightEdit.getText().toString();
                    if (!weightString.isEmpty()) {
                        vehicleOptions.setWeight(Float.parseFloat(weightString));
                    }

                    EditText axleWeightEdit = (EditText) dialog.findViewById(R.id.set_axle_weight);
                    String axleWeightString = axleWeightEdit.getText().toString();
                    if (!axleWeightString.isEmpty()) {
                        vehicleOptions.setAxleWeight(Float.parseFloat(axleWeightString));
                    }

                    EditText maxWeightEdit = (EditText) dialog.findViewById(R.id.set_max_weight);
                    String maxWeightString = maxWeightEdit.getText().toString();
                    if (!maxWeightString.isEmpty()) {
                        vehicleOptions.setMaxWeight(Float.parseFloat(maxWeightString));
                    }

                    EditText heightEdit = (EditText) dialog.findViewById(R.id.set_height);
                    String heightString = heightEdit.getText().toString();
                    if (!heightString.isEmpty()) {
                        vehicleOptions.setHeight(Float.parseFloat(heightString));
                    }

                    EditText widthEdit = (EditText) dialog.findViewById(R.id.set_width);
                    String widthString = widthEdit.getText().toString();
                    if (!widthString.isEmpty()) {
                        vehicleOptions.setWidth(Float.parseFloat(widthString));
                    }

                    EditText lengthEdit = (EditText) dialog.findViewById(R.id.set_length);
                    String lengthString = lengthEdit.getText().toString();
                    if (!lengthString.isEmpty()) {
                        vehicleOptions.setLength(Float.parseFloat(lengthString));
                    }

                    EditText payloadEdit = (EditText) dialog.findViewById(R.id.set_payload);
                    String payloadString = payloadEdit.getText().toString();
                    if (!payloadString.isEmpty()) {
                        vehicleOptions.setPayload(Float.parseFloat(payloadString));
                    }

                    EditText ecoClassEdit = (EditText) dialog.findViewById(R.id.set_eco_class);
                    String ecoClassString = ecoClassEdit.getText().toString();
                    if (!ecoClassString.isEmpty()) {
                        vehicleOptions.setEcoClass(Integer.parseInt(ecoClassString));
                    }

                    CheckBox hasTrailerCheckBox = (CheckBox) dialog.findViewById(R.id.set_has_trailer);
                    vehicleOptions.setHasTrailer(hasTrailerCheckBox.isChecked());

                    listener.onVehicleOptionsSelected(vehicleOptions);
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();

        RadioGroup vehicleTypeRadioGroup = (RadioGroup) dialog.findViewById(R.id.set_vehicle_type);
        if (currentVehicleOptions.getVehicleType() == VehicleType.DEFAULT) {
            vehicleTypeRadioGroup.check(R.id.set_vehicle_type_default);
        } else if (currentVehicleOptions.getVehicleType() == VehicleType.TAXI) {
            vehicleTypeRadioGroup.check(R.id.set_vehicle_type_taxi);
        } else if (currentVehicleOptions.getVehicleType() == VehicleType.TRUCK) {
            vehicleTypeRadioGroup.check(R.id.set_vehicle_type_truck);
        }

        EditText weightEdit = (EditText) dialog.findViewById(R.id.set_weight);
        if (currentVehicleOptions.getWeight() != null) {
            weightEdit.setText(currentVehicleOptions.getWeight().toString());
        }

        EditText axleWeightEdit = (EditText) dialog.findViewById(R.id.set_axle_weight);
        if (currentVehicleOptions.getAxleWeight() != null) {
            axleWeightEdit.setText(currentVehicleOptions.getAxleWeight().toString());
        }

        EditText maxWeightEdit = (EditText) dialog.findViewById(R.id.set_max_weight);
        if (currentVehicleOptions.getMaxWeight() != null) {
            maxWeightEdit.setText(currentVehicleOptions.getMaxWeight().toString());
        }

        EditText heightEdit = (EditText) dialog.findViewById(R.id.set_height);
        if (currentVehicleOptions.getHeight() != null) {
            heightEdit.setText(currentVehicleOptions.getHeight().toString());
        }

        EditText widthEdit = (EditText) dialog.findViewById(R.id.set_width);
        if (currentVehicleOptions.getWidth() != null) {
            widthEdit.setText(currentVehicleOptions.getWidth().toString());
        }

        EditText lengthEdit = (EditText) dialog.findViewById(R.id.set_length);
        if (currentVehicleOptions.getLength() != null) {
            lengthEdit.setText(currentVehicleOptions.getLength().toString());
        }

        EditText payloadEdit = (EditText) dialog.findViewById(R.id.set_payload);
        if (currentVehicleOptions.getPayload() != null) {
            payloadEdit.setText(currentVehicleOptions.getPayload().toString());
        }

        EditText ecoClassEdit = (EditText) dialog.findViewById(R.id.set_eco_class);
        if (currentVehicleOptions.getEcoClass() != null) {
            ecoClassEdit.setText(currentVehicleOptions.getEcoClass().toString());
        }

        CheckBox hasTrailerCheckBox = (CheckBox) dialog.findViewById(R.id.set_has_trailer);
        if (currentVehicleOptions.getHasTrailer() != null) {
            hasTrailerCheckBox.setChecked(currentVehicleOptions.getHasTrailer());
        }
    }
}
