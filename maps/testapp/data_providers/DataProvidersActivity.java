package com.yandex.maps.testapp.data_providers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.settings.SharedPreferencesConsts;

import java.util.Arrays;

public class DataProvidersActivity extends TestAppActivity {

    private void setupSpinnerAdapter(Spinner spinner)
    {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.dataproviders_choices,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupDataProviderSelectionListener(Spinner spinner, String preferencesKey)
    {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                SharedPreferences sPref = getSharedPreferences(SharedPreferencesConsts.I18N_PREFS, MODE_PRIVATE);
                String[] spinnerChoices = getResources().getStringArray(R.array.dataproviders_choices);
                String currentValue = sPref.getString(preferencesKey, spinnerChoices[0]);
                String newValue = (String)adapterView.getItemAtPosition(i);
                Editor editor = sPref.edit();
                editor.putString(preferencesKey, newValue);
                editor.apply();
                if(!currentValue.equals(newValue))
                    onRestartDependentSettingChanged_.show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void restoreDataProviderSelection(Spinner spinner, String preferencesKey)
    {
        SharedPreferences sPref = getSharedPreferences(SharedPreferencesConsts.I18N_PREFS, MODE_PRIVATE);
        String[] spinnerChoices = getResources().getStringArray(R.array.dataproviders_choices);
        String value = sPref.getString(preferencesKey, spinnerChoices[0]);
        spinner.setSelection(Arrays.asList(spinnerChoices).indexOf(value), false);
    }

    private void setupDataProviderSpinner(Spinner spinner, String preferencesKey)
    {
        setupSpinnerAdapter(spinner);
        restoreDataProviderSelection(spinner, preferencesKey);
        setupDataProviderSelectionListener(spinner, preferencesKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_providers);
        drivingSpinner_ = (Spinner)findViewById(R.id.spinner_driving);
        mapSpinner_ = (Spinner)findViewById(R.id.spinner_map);
        searchSpinner_ = (Spinner)findViewById(R.id.spinner_search);
        suggestSpinner_ = (Spinner)findViewById(R.id.spinner_suggest);

        setupDataProviderSpinner(drivingSpinner_, DataProvidersConsts.DRIVING_DATAPROVIDER_KEY);
        setupDataProviderSpinner(mapSpinner_, DataProvidersConsts.MAP_DATAPROVIDER_KEY);
        setupDataProviderSpinner(searchSpinner_, DataProvidersConsts.SEARCH_DATAPROVIDER_KEY);
        setupDataProviderSpinner(suggestSpinner_, DataProvidersConsts.SUGGEST_DATAPROVIDER_KEY);

//      Create alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restart the app")
                .setMessage("The changes will take effect after a restart")
                .setNegativeButton("I understand and won't file a bug report",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        onRestartDependentSettingChanged_ = builder.create();
    }

    @Override
    protected void onStartImpl() {

    }

    @Override
    protected void onStopImpl() {

    }

    Spinner drivingSpinner_;
    Spinner mapSpinner_;
    Spinner searchSpinner_;
    Spinner suggestSpinner_;

    AlertDialog onRestartDependentSettingChanged_;
}
