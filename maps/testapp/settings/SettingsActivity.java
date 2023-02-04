package com.yandex.maps.testapp.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.i18n.SystemOfMeasurement;
import com.yandex.runtime.i18n.TimeFormat;

public class SettingsActivity extends TestAppActivity {
    void setSomRadioButton(SystemOfMeasurement som) {
        RadioGroup group = (RadioGroup)findViewById(R.id.settings_som);
        switch (som) {
            case DEFAULT:
                group.check(R.id.settings_default_som);
                break;
            case METRIC:
                group.check(R.id.settings_metric_som);
                break;
            case IMPERIAL:
                group.check(R.id.settings_imperial_som);
        }
    }

    void setTimeFormatRadioButtom(TimeFormat tf) {
        RadioGroup group = (RadioGroup)findViewById(R.id.settings_tf);
        switch (tf) {
            case DEFAULT:
                group.check(R.id.settings_default_tf);
                break;
            case H12:
                group.check(R.id.settings_12h_tf);
                break;
            case H24:
                group.check(R.id.settings_24h_tf);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        countryText_ = (TextView)findViewById(R.id.country_text);
        languageText_ = (TextView)findViewById(R.id.language_text);
        customCountry_ = (EditText)findViewById(R.id.custom_country);
        customLanguage_ = (EditText)findViewById(R.id.custom_language);
        saveLocaleButton_ = (Button)findViewById(R.id.save_locale_button);
        customLocaleSwitch_ = (ToggleButton)findViewById(R.id.custom_locale_switch);

        SharedPreferences sPref = getSharedPreferences(SharedPreferencesConsts.I18N_PREFS, MODE_PRIVATE);
        String locale = sPref.getString(SharedPreferencesConsts.LOCALE_KEY, "");
        if (locale == "") {
            customLocaleSwitch_.setChecked(false);
            setCustomLocaleViewsEnabled(false);
            String[] localeComponents = I18nManagerFactory.getLocale().split("_");
            customLanguage_.setText(localeComponents[0]);
            customCountry_.setText(localeComponents[1]);
        } else {
            customLocaleSwitch_.setChecked(true);
            setCustomLocaleViewsEnabled(true);
            String[] components = locale.split("_");
            customLanguage_.setText(components[0]);
            customCountry_.setText(components[1]);
        }

//      Create alerts
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restart the app")
            .setMessage("The changes will take effect after a restart")
            .setNegativeButton("I understand and won't file a bug report",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        onLocaleUpdated_ = builder.create();

        builder.setTitle("Incorrect locale")
            .setMessage("Language is incorrect!")
            .setNegativeButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
        onIncorrectLocale_ = builder.create();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}

    @Override
    public void onResume() {
        super.onResume();
        setSomRadioButton(I18nManagerFactory.getI18nManagerInstance().getSom());
        setTimeFormatRadioButtom(I18nManagerFactory.getI18nManagerInstance().getTimeFormat());
    }

    public void onCustomLocaleChecked(View view) {
        if (customLocaleSwitch_.isChecked()) {
            setCustomLocaleViewsEnabled(true);
            updateLocale(customLanguage_.getText().toString(), customCountry_.getText().toString());
        } else {
            setCustomLocaleViewsEnabled(false);
            resetLocale();
        }
    }

    public void onSomChange(View view) {
        int selectedId = view.getId();
        SystemOfMeasurement som;
        if (selectedId == R.id.settings_default_som) {
            som = SystemOfMeasurement.DEFAULT;
        } else if (selectedId == R.id.settings_metric_som) {
            som = SystemOfMeasurement.METRIC;
        } else if (selectedId == R.id.settings_imperial_som) {
            som = SystemOfMeasurement.IMPERIAL;
        } else {
            return;
        }
        I18nManagerFactory.getI18nManagerInstance().setSom(som);
    }

    public void onTimeFormatChange(View view) {
        int selectedId = view.getId();
        TimeFormat tf;
        if (selectedId == R.id.settings_default_tf) {
            tf = TimeFormat.DEFAULT;
        } else if (selectedId == R.id.settings_12h_tf) {
            tf = TimeFormat.H12;
        } else if (selectedId == R.id.settings_24h_tf) {
            tf = TimeFormat.H24;
        } else {
            return;
        }
        I18nManagerFactory.getI18nManagerInstance().setTimeFormat(tf);
    }

    public void onSaveLocaleButton(View view) {
        updateLocale(customLanguage_.getText().toString(), customCountry_.getText().toString());
    }

    private void updateLocale(String language, String country)
    {
        SharedPreferences sPref = getSharedPreferences(SharedPreferencesConsts.I18N_PREFS, MODE_PRIVATE);
        Editor editor = sPref.edit();
        if (!language.contains("_"))
        {
            editor.putString(SharedPreferencesConsts.LOCALE_KEY, language + '_' + country);
            editor.apply();
            onLocaleUpdated_.show();
        } else
            onIncorrectLocale_.show();
    }

    private void resetLocale()
    {
        SharedPreferences sPref = getSharedPreferences(SharedPreferencesConsts.I18N_PREFS, MODE_PRIVATE);
        Editor editor = sPref.edit();
        String[] localeComponents = I18nManagerFactory.getLocale().split("_");
        customLanguage_.setText(localeComponents[0]);
        customCountry_.setText(localeComponents[1]);

        editor.remove(SharedPreferencesConsts.LOCALE_KEY);
        editor.apply();
    }

    private void setCustomLocaleViewsEnabled(boolean value) {
        countryText_.setEnabled(value);
        languageText_.setEnabled(value);
        customCountry_.setEnabled(value);
        customLanguage_.setEnabled(value);
        saveLocaleButton_.setEnabled(value);
    }

    TextView countryText_;
    TextView languageText_;
    EditText customCountry_;
    EditText customLanguage_;
    Button saveLocaleButton_;
    ToggleButton customLocaleSwitch_;

    AlertDialog onLocaleUpdated_;
    AlertDialog onIncorrectLocale_;

}
