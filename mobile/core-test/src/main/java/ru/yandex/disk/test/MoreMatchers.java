package ru.yandex.disk.test;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import org.hamcrest.Matcher;
import ru.yandex.disk.MoreAsserts2;
import ru.yandex.disk.asyncbitmap.Bitmaps;
import ru.yandex.util.Path;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;

/**
 * Container for Matchers related to Android.
 */
public class MoreMatchers {

    private MoreMatchers() {
    }

    public static Bitmap eq(Bitmap wanted) {
        return argThat(new AbstractEquals<Bitmap>(wanted) {

            @Override
            protected boolean equals(Bitmap wanted, Bitmap actual) {
                return Bitmaps.equals(wanted, actual);
            }

        });
    }

    public static Bundle eq(Bundle wanted) {
        return argThat(new AbstractEquals<Bundle>(wanted) {

            @Override
            protected boolean equals(Bundle wanted, Bundle actual) {
                return MoreAsserts2.isBundlesEqual(wanted, actual);
            }

        });
    }

    public static Context anyContext() {
        return any(Context.class);
    }

    public static Bundle anyBundle() {
        return any(Bundle.class);
    }

    public static File anyFile() {
        return any(File.class);
    }

    public static Uri anyUri() {
        return  any(Uri.class);
    }

    public static String[] anyStrings() {
        return any(String[].class);
    }

    public static ContentValues[] anyContentValuesArray() {
        return any(ContentValues[].class);
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> anyCollection() {
        return any(Collection.class);
    }

    public static <T> Matcher<List<T>> equalToList(T... arr) {
        return equalTo(Arrays.asList(arr));
    }

    public static Path anyPath() {
        return any(Path.class);
    }
}
