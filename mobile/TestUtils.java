package com.yandex.mail;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.squareup.sqldelight.android.AndroidSqliteDriver;
import com.squareup.sqldelight.db.SqlCursor;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.tools.TestWorkerFactory;
import com.yandex.mail.util.NonInstantiableException;

import java.io.IOException;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUtils {

    private TestUtils() {
        throw new NonInstantiableException();
    }

    @NonNull
    public static Bundle serializeAndDeserializeState(@NonNull Bundle state) {
        Parcel parcel = Parcel.obtain();
        state.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        byte[] binaryState = parcel.marshall();

        Parcel recreatedParcel = Parcel.obtain();
        recreatedParcel.unmarshall(binaryState, 0, binaryState.length);
        recreatedParcel.setDataPosition(0);

        Bundle recreatedBundle = new Bundle();
        recreatedBundle.readFromParcel(recreatedParcel);

        return recreatedBundle;
    }

    @NonNull
    public static <T extends Parcelable> T serializeAndDeserialize(@NonNull T parcelable) {
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(parcelable, 0);
        parcel.setDataPosition(0);

        byte[] binaryState = parcel.marshall();

        Parcel recreatedParcel = Parcel.obtain();
        recreatedParcel.unmarshall(binaryState, 0, binaryState.length);
        recreatedParcel.setDataPosition(0);

        return recreatedParcel.readParcelable(parcelable.getClass().getClassLoader());
    }

    public static int getTableSize(@NonNull AndroidSqliteDriver driver, @NonNull String tableName) {
        SqlCursor cursor = driver.executeQuery(
                null,
                "SELECT COUNT(*) FROM " + tableName,
                0,
                null
        );
        cursor.next();
        int size = cursor.getLong(0).intValue();
        try {
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * Method that test that particular intent is started.
     * This is needed to unify all such tests and to be able to migrate to sdk = 26 in tests easily later.
     *
     * @param action service action that should be started
     */
    public static void assertWorkerStarted(@NonNull TestWorkerFactory workerFactory, @NonNull String action) {
        assertThat(workerFactory.getAllStartedWorkers()).extracting(worker -> worker.getInputString(Constants.ACTION_EXTRA)).containsOnlyOnce(action);
    }

    /**
     * Method that test that no services is currently started.
     * This is needed to unify all such tests and to be able to migrate to sdk = 26 in tests easily later.
     */
    public static void assertWorkerNotStarted(@NonNull TestWorkerFactory workerFactory, @NonNull String action) {
        assertThat(workerFactory.getAllStartedWorkers()).extracting(worker -> worker.getInputString(Constants.ACTION_EXTRA)).doesNotContain(action);
    }
}
