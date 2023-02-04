package com.yandex.maps.testapp.apiKeySettings;

import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.yandex.maps.testapp.apiKeySettings.ApiKeyConsts;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

public class ApiKeySettingsActivity extends TestAppActivity {
    void updateRadioButtonValues() {
        SharedPreferences sPref = getSharedPreferences(ApiKeyConsts.API_KEY_PREFS, MODE_PRIVATE);
        String apiKeyType = sPref.getString(ApiKeyConsts.API_KEY_TYPE_KEY, "");
        if (apiKeyType.equals(ApiKeyConsts.YANDEX_STRING)) {
            yandexKeyButton_.toggle();
        } else if (apiKeyType.equals(ApiKeyConsts.COMMERCIAL_STRING)) {
            commercialKeyButton_.toggle();
        } else if (apiKeyType.equals(ApiKeyConsts.FREE_STRING)) {
            freeKeyButton_.toggle();
        } else {
            yandexKeyButton_.toggle();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.api_key_settings);
        yandexKeyButton_ = (RadioButton)findViewById(R.id.settings_api_key_yandex);
        commercialKeyButton_ = (RadioButton)findViewById(R.id.settings_api_key_commercial);
        freeKeyButton_ = (RadioButton)findViewById(R.id.settings_api_key_free);
        restartAppText_ = (TextView)findViewById(R.id.label_restart_app_warning);
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
        updateRadioButtonValues();
    }

    public void onApiKeyChange(View view) {
        int selectedId = view.getId();
        String apiKeyType;
        if (selectedId == R.id.settings_api_key_yandex) {
            apiKeyType = ApiKeyConsts.YANDEX_STRING;
        } else if (selectedId == R.id.settings_api_key_commercial) {
            apiKeyType = ApiKeyConsts.COMMERCIAL_STRING;
        } else if (selectedId == R.id.settings_api_key_free) {
            apiKeyType = ApiKeyConsts.FREE_STRING;
        } else {
            apiKeyType = ApiKeyConsts.YANDEX_STRING;
        }
        SharedPreferences sPref = getSharedPreferences(ApiKeyConsts.API_KEY_PREFS, MODE_PRIVATE);
        Editor editor = sPref.edit();
        editor.putString(ApiKeyConsts.API_KEY_TYPE_KEY, apiKeyType);
        editor.apply();
        restartAppText_.setVisibility(View.VISIBLE);
    }

    RadioButton yandexKeyButton_;
    RadioButton commercialKeyButton_;
    RadioButton freeKeyButton_;
    TextView restartAppText_;
}
