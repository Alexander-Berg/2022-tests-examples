package com.yandex.maps.testapp.map;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.DialogFragment;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class MapCustomizationDialog<StyleTarget> {
    private static final String LAST_USED_CUSTOMIZATION_URL_KEY = "customization_url";
    private static final String CUSTOMIZATION_SETTINGS_FILE = "customization_file";

    public interface StyleHandler<StyleTarget> {
        void applyStyle(StyleTarget styleTarget, final String style);
        void saveStyle(StyleTarget styleTarget, final String style);
    }

    private StyleTarget styleTarget;
    private Map<StyleTarget, String> styles;
    private Map<StyleTarget, Integer> templateMenuSources;

    private FragmentActivity context;
    private StyleHandler<StyleTarget> styleHandler;

    public MapCustomizationDialog(FragmentActivity context, StyleHandler<StyleTarget> styleHandler, Map<StyleTarget, Integer> templateMenuSources) {
        this.context = context;
        this.styleHandler = styleHandler;

        this.styles = new HashMap<>();
        this.templateMenuSources = templateMenuSources;
    }

    public void setStyleText(StyleTarget styleTarget, String style) {
        styles.put(styleTarget, style);
    }

    public void show(StyleTarget styleTarget) {
        show(styleTarget, true);
    }

    public void show(StyleTarget styleTarget, boolean showSaveButton) {
        CustomizationDialog<StyleTarget> dialog = new CustomizationDialog<>();
        this.styleTarget = styleTarget;
        dialog.showSaveButton = showSaveButton;
        dialog.weakStorage = new WeakReference<>(this);
        dialog.show(context.getSupportFragmentManager(), "customization_dialog");
    }

    public String getStyleText() {
        String text = styles.get(styleTarget);
        if (text == null)
            return "";
        return text;
    }

    private void applyStyle(String style) {
        styles.put(styleTarget, style);
        styleHandler.applyStyle(styleTarget, getStyleText());
    }

    private void saveStyle(String style) {
        styles.put(styleTarget, style);
        styleHandler.saveStyle(styleTarget, getStyleText());
    }

    private String makeLastUsedUrlKey() {
        return styleTarget.toString() + "#" + LAST_USED_CUSTOMIZATION_URL_KEY;
    }

    private final Consumer<String> saveLastUsedUrl = urlText -> {
        SharedPreferences sharedPref = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(makeLastUsedUrlKey(), urlText);
        editor.apply();
    };

    private String getLastUsedUrl() {
        SharedPreferences sharedPref = getSharedPreferences(context);
        return sharedPref.getString(makeLastUsedUrlKey(), "");
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(CUSTOMIZATION_SETTINGS_FILE, Context.MODE_PRIVATE);
    }

    public static class CustomizationDialog<StyleTarget> extends DialogFragment {
        private Consumer<String> onJSONLoadingSuccess = json -> {
            if (json != null) {
                EditText styleEditText = getDialog().findViewById(R.id.style_edit_text);
                styleEditText.setText(json);
            }
        };

        private WeakReference<MapCustomizationDialog<StyleTarget>> weakStorage = new WeakReference<>(null);
        private boolean showSaveButton = true;

        MapCustomizationDialog<StyleTarget> getStorage() {
            MapCustomizationDialog<StyleTarget> storage = weakStorage.get();
            if (storage == null)
                dismiss();
            return storage;
        }

        @Override
        public void onDestroyView() {
            // cleanup to avoid memory leak of the dialog
            onJSONLoadingSuccess = null;
            super.onDestroyView();
        }

        private void onStyleEditTextChanged() {
            MapCustomizationDialog<StyleTarget> storage = getStorage();
            if (storage == null)
                return;

            EditText styleEditText = getDialog().findViewById(R.id.style_edit_text);
            boolean textChanged = !styleEditText.getText().toString().equals(storage.getStyleText());

            getDialog().findViewById(R.id.apply_customization_button).setEnabled(textChanged);
            getDialog().findViewById(R.id.save_customization_button).setEnabled(textChanged);
            getDialog().findViewById(R.id.reset_customization_button).setEnabled(textChanged);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MapCustomizationDialog<StyleTarget> storage = getStorage();
            if (storage == null)
                return null;

            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            View contentView = inflater.inflate(R.layout.customization_dialog_layout, null);

            TextView header = contentView.findViewById(R.id.style_target);
            header.setText(storage.styleTarget.toString());

            EditText styleEditText = contentView.findViewById(R.id.style_edit_text);
            styleEditText.setText(storage.getStyleText());
            styleEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        onStyleEditTextChanged();
                    }
                }
            );

            Button templateButton = contentView.findViewById(R.id.styler_templates_button);
            if (storage.templateMenuSources.get(storage.styleTarget) == null)
                templateButton.setVisibility(View.GONE);
            else
                templateButton.setVisibility(View.VISIBLE);

            templateButton.setOnClickListener(v -> {
                MapCustomizationDialog<StyleTarget> storage1 = getStorage();
                if (storage1 == null)
                    return;
                PopupMenu popup = new PopupMenu(storage1.context, v);
                MenuInflater inflater1 = popup.getMenuInflater();
                popup.setOnMenuItemClickListener(item -> {
                    MapCustomizationDialog<StyleTarget> storage2 = getStorage();
                    if (storage2 == null)
                        return true;
                    switch (item.getItemId()) {
                        case R.id.taxi_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.taxi_style));
                            return true;
                        case R.id.transport_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.transport_style));
                            return true;
                        case R.id.poi_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.poi_style));
                            return true;
                        case R.id.xray_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.xray_style));
                            return true;
                        case R.id.hidden_points_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.hidden_points_style));
                            return true;
                        case R.id.colored_icon_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.colored_icon_style));
                            return true;
                        case R.id.colored_polyline_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.colored_polyline_style));
                            return true;
                        case R.id.earthquakes_heatmap_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.earthquakes_heatmap_style));
                            return true;
                        case R.id.surge_heatmap_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.surge_heatmap_style));
                            return true;
                        case R.id.empty_style:
                            styleEditText.setText("");
                            return true;
                        case R.id.carparks_drive_style:
                            styleEditText.setText(Utils.readResourceAsString(storage2.context, R.raw.carparks_drive_style));
                            return true;
                        default:
                            return false;
                    }
                });

                Integer templates = storage1.templateMenuSources.get(storage1.styleTarget);
                if (templates != null)
                    inflater1.inflate(templates, popup.getMenu());

                popup.show();
            });

            contentView.findViewById(R.id.styler_url_button).setOnClickListener(v -> {
                MapCustomizationDialog<StyleTarget> storage1 = getStorage();
                if (storage1 == null)
                    return;
                JsonLoaderDialog.show(
                        storage1.getLastUsedUrl(),
                        storage1.context,
                        onJSONLoadingSuccess,
                        storage1.saveLastUsedUrl);
            });

            contentView.findViewById(R.id.apply_customization_button).setOnClickListener(v -> {
                MapCustomizationDialog<StyleTarget> storage1 = getStorage();
                if (storage1 != null)
                    storage1.applyStyle(styleEditText.getText().toString());
                dismiss();
            });

            contentView.findViewById(R.id.save_customization_button).setOnClickListener(v -> {
                MapCustomizationDialog<StyleTarget> storage1 = getStorage();
                if (storage1 != null)
                    storage1.saveStyle(styleEditText.getText().toString());
                dismiss();
            });

            contentView.findViewById(R.id.cancel_customization_button).setOnClickListener(v -> dismiss());

            contentView.findViewById(R.id.reset_customization_button).setOnClickListener(v -> {
                MapCustomizationDialog<StyleTarget> storage1 = getStorage();
                if (storage1 != null)
                    styleEditText.setText(storage1.getStyleText());
            });

            Button saveButton = contentView.findViewById(R.id.save_customization_button);
            if (showSaveButton)
                saveButton.setVisibility(View.VISIBLE);
            else
                saveButton.setVisibility(View.GONE);

            return contentView;
        }
    }
}
