package com.yandex.maps.testapp.masstransit_routing;


import android.app.AlertDialog;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.widget.ListView;

import com.yandex.maps.testapp.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class VehicleTypesProvider {

    interface Listener {
        void onVehicleTypesSelected(@NotNull List<String> types);
    }

    static private final
    List<String> AVAILABLE_TYPES = Arrays.asList(
        "bus",
        "minibus",
        "trolleybus",
        "tramway",
        "underground",
        "railway",
        "suburban"
    );

    private Context ctx;
    private Set<String> selected = new HashSet<>();
    private Listener listener;

    VehicleTypesProvider(@NotNull Context ctx, @NotNull Listener listener) {
        this.ctx = ctx;
        this.listener = listener;
    }

    void poll() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.set_masstransit_filters);

        Set<String> deselected = new HashSet<>(AVAILABLE_TYPES);
        deselected.removeAll(selected);

        List<String> all = new ArrayList<>(selected);
        all.addAll(deselected);

        final boolean[] flags = new boolean[all.size()];
        Arrays.fill(flags, 0, selected.size(), true);

        builder.setMultiChoiceItems(
            all.toArray(new String[all.size()]),
            flags,
            (dialog, which, isChecked) -> {});

        builder.setNegativeButton(
            R.string.masstransit_vehicle_filter_uncheck_all, null);
        builder.setPositiveButton(
            R.string.masstransit_vehicle_filter_apply, null);

        final AlertDialog dialog = builder.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(dummy -> {
            selected.clear();
            ListView view = dialog.getListView();
            SparseBooleanArray checked = view.getCheckedItemPositions();
            for (int i = 0; i < view.getCount(); ++i) {
                if (checked.get(i)) {
                    selected.add(all.get(i));
                }
            }
            dialog.dismiss();

            listener.onVehicleTypesSelected(new ArrayList<>(selected));
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(dummy -> {
            Arrays.fill(flags, false);
            dialog.getListView().clearChoices();
            dialog.getListView().invalidateViews();
        });
    }
}
