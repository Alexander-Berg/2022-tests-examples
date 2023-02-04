package com.yandex.maps.testapp.map;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import android.util.Log;
import android.view.View;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.DialogFragment;

import com.yandex.maps.testapp.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;

public class JsonLoaderDialog extends DialogFragment {
    private JsonLoadingTask loadingTask = null;

    private boolean canBeDismissed = false;
    private boolean shouldBeDismissed = false;

    private String defaultUrlToShow;
    private WeakReference<Consumer<String>> onSuccess = new WeakReference<>(null);
    private WeakReference<Consumer<String>> saveLastUsedUrl = new WeakReference<>(null);

    private Consumer<String> onLoadingSuccess;
    private Consumer<String> onLoadingError;

    private void safeDismiss() {
        if (canBeDismissed)
            dismiss();
        shouldBeDismissed = !canBeDismissed;
    }

    private Consumer<String> safeRunInUiThread(Consumer<String> task) {
        return data -> {
            FragmentActivity activity = getActivity();
            if (activity == null)
                dismiss();
            else
                activity.runOnUiThread(() -> task.accept(data));
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (onSuccess.get() == null && saveLastUsedUrl.get() == null) {
            dismiss();
            return null;
        }
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        final View contentView = inflater.inflate(R.layout.customization_url_layout, null);

        final EditText urlEditText = contentView.findViewById(R.id.url_edit_text);
        urlEditText.post(() -> urlEditText.setText(defaultUrlToShow));

        final TextView errorView = contentView.findViewById(R.id.error_text_view);

        final Button downloadButton = contentView.findViewById(R.id.download_customization_button);
        downloadButton.setOnClickListener(v -> {
            final String urlText = urlEditText.getText().toString();
            Consumer<String> saveLastUsedUrl = this.saveLastUsedUrl.get();
            if (saveLastUsedUrl == null) {
                safeDismiss();
                return;
            }
            saveLastUsedUrl.accept(urlText);

            downloadButton.setEnabled(false);
            urlEditText.setEnabled(false);
            errorView.setVisibility(View.VISIBLE);
            errorView.setText("Loading from '" + urlText + "'");

            if (loadingTask != null)
                loadingTask.cancel(true);

            loadingTask = new JsonLoadingTask(urlText,
                    onLoadingSuccess = safeRunInUiThread(json -> {
                        downloadButton.setEnabled(true);
                        urlEditText.setEnabled(true);
                        safeDismiss();
                        Consumer<String> onSuccess = this.onSuccess.get();
                        if (onSuccess != null)
                            onSuccess.accept(json);
                    }),
                    onLoadingError = safeRunInUiThread(errorMessage -> {
                        downloadButton.setEnabled(true);
                        urlEditText.setEnabled(true);
                        errorView.setVisibility(View.VISIBLE);
                        errorView.setText(errorMessage);
                    })
            );
            loadingTask.execute();
        });

        final Button cancelButton = contentView.findViewById(R.id.cancel_customization_button);
        cancelButton.setOnClickListener(v -> {
            safeDismiss();
            cancelTask();
        });

        return contentView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        canBeDismissed = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        canBeDismissed = true;
        if (shouldBeDismissed)
            safeDismiss();
    }

    private void cancelTask() {
        if (loadingTask != null) {
            loadingTask.cancel(true);
            loadingTask = null;
        }
    }

    @Override
    public void onDestroyView() {
        // cleanup to avoid memory leak of the dialog
        cancelTask();
        onLoadingSuccess = null;
        onLoadingError = null;
        super.onDestroyView();
    }

    public static void show(String defaultUrlToShow, FragmentActivity activity, final Consumer<String> onSuccess, final Consumer<String> saveLastUsedUrl) {
        JsonLoaderDialog dialog = new JsonLoaderDialog();

        dialog.defaultUrlToShow = defaultUrlToShow;
        dialog.onSuccess = new WeakReference<>(onSuccess);
        dialog.saveLastUsedUrl = new WeakReference<>(saveLastUsedUrl);

        dialog.show(activity.getSupportFragmentManager(), "json_loader_dialog");
    }

    private class JsonLoadingTask extends  AsyncTask<Void, Void, String> {

        private String urlText;
        private Consumer<String> onSuccess;
        private Consumer<String> onError;
        private volatile boolean loadingFailed = false;

        JsonLoadingTask(String urlText, Consumer<String> onSuccess, Consumer<String> onError) {
            this.urlText = urlText;
            this.onSuccess = onSuccess;
            this.onError = onError;
        }

        @Override
        protected String doInBackground(Void... voids) {

            BufferedReader reader = null;
            try {
                URL url = new URL(urlText);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(2000);

                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }

                return stringBuilder.toString();

            } catch (Exception e) {
                loadingFailed = true;
                return "Error: " + e.toString();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e("JsonLoaderDialog",
                                "Failed to close buffered reader: " + e.toString());
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String backgroundResult) {
            try {
                if (loadingFailed) {
                    if (onError != null)
                        onError.accept(backgroundResult);
                    return;
                }

                if (onSuccess != null) {
                    onSuccess.accept(backgroundResult);
                }
            } finally {
                urlText = null;
                onSuccess = null;
                onError = null;
            }
        }
    }
}
