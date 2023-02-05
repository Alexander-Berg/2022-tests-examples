package com.yandex.mail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.WrapperListAdapter;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class TestUtil {

    private TestUtil() {
        throw new IllegalStateException("No instances!");
    }

    @NonNull
    private static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    @NonNull
    public static UiTestsApplication getApplication() {
        return (UiTestsApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
    }

    @NonNull
    public static Resources getResources() {
        return getApplication().getApplicationContext().getResources();
    }

    @NonNull
    public static Activity getCurrentActivity() {
        final AtomicReference<Activity> activity = new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> {
                    for (Activity a : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                        activity.set(a);
                        break;
                    }
                }
        );

        assertThat("Activity was not found", activity.get(), notNullValue());

        return activity.get();
    }

    @NonNull
    public static <T extends Activity> T getCurrentActivityAs(@NonNull Class<T> activityClass) {
        return activityClass.cast(getCurrentActivity());
    }

    @NonNull
    public static String randomText(int count) {
        return RandomStringUtils.randomAlphabetic(count);
    }

    @NonNull
    public static Handler getMainThreadHandler() {
        return MAIN_THREAD_HANDLER;
    }

    public static boolean deleteFileByUri(@NonNull Uri fileUri) {
        return new File(URI.create(fileUri.toString())).delete();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> T getAdapterFromListView(@NonNull ListView listView) {
        Adapter listAdapter = listView.getAdapter();
        if (listAdapter instanceof WrapperListAdapter) {
            return (T) ((WrapperListAdapter) listAdapter).getWrappedAdapter();
        } else {
            return (T) listAdapter;
        }
    }

    @SuppressLint("NewApi")
    public static void saveFakePhoto(@NonNull Uri fileUri) {
        Bitmap bitmap = BitmapFactory.decodeResource(
                InstrumentationRegistry.getTargetContext().getResources(),
                R.mipmap.ic_launcher_mail
        );

        try (
                ParcelFileDescriptor fileDescriptor = TestUtil.getApplication()
                        .getContentResolver()
                        .openFileDescriptor(fileUri, "rwt");
                FileOutputStream fileOutputStream =
                        new FileOutputStream(fileDescriptor.getFileDescriptor())
        ) {
            // Put fake image to requested path
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static Uri createTempFile(@NonNull String fileTitle) {
        File fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        fileDir.mkdir();

        try {
            File image = File.createTempFile(fileTitle, ".jpg", fileDir);
            return Uri.fromFile(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setResultForCurrentActivity(int result, @NonNull Intent intent) {
        new Thread(
                () -> {
                    Activity activity = getCurrentActivity();
                    TestUtil.getMainThreadHandler().post(
                            () -> {
                                activity.setResult(result, intent);
                                activity.finish();
                            }
                    );
                }
        ).start();
    }

    public static void setResultForCurrentActivity(int result) {
        new Thread(
                () -> {
                    Activity activity = getCurrentActivity();
                    TestUtil.getMainThreadHandler().post(
                            () -> {
                                activity.setResult(result);
                                activity.finish();
                            }
                    );
                }
        ).start();
    }

    @NonNull
    public static Uri createTextFile(@NonNull String fileName, final int stringsNumbers) {
        File fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        fileDir.mkdir();
        File textFile;

        try {
            textFile = File.createTempFile(fileName, ".txt", fileDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter buf = new BufferedWriter(new FileWriter(textFile, true))) {
            for (int i = 0; i < stringsNumbers; i++) {
                buf.append("Reach out and touch faith. Your own personal Jesus. Someone to hear your prayers. ")
                        .append("Someone who cares. Your own personal Jesus. Someone to hear your prayers. ")
                        .append("Someone who's there. ");
                buf.newLine();
            }
            return Uri.fromFile(textFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static Date convertStringToDate(@NonNull String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);
        try {
            return new Date(NANOSECONDS.toMicros((dateFormat.parse(dateString)).getTime()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
