package ru.yandex.disk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import ru.yandex.disk.asyncbitmap.Bitmaps;

import java.util.Arrays;

import static junit.framework.Assert.*;

public class MoreAsserts2 {

    public static void assertEqualStrings(CharSequence expected, CharSequence actual) {
        String expectedString = expected != null ? expected.toString() : null;
        String actualString = actual != null ? actual.toString() : null;
        Assert.assertEquals(expectedString, actualString);
    }

    public static void assertContains(CharSequence actual, Object... substrings) {
        assertContains(actual.toString(), substrings);
    }

    private static void assertContains(String actual, Object... substrings) {
        for (Object substring : substrings) {
            String message = "expect string '" + actual +
                    "' contains " + Arrays.toString(substrings);
            Assert.assertTrue(message, actual.contains(substring.toString()));
        }
    }

    public static void assertEqualsString(Context context, int resId, CharSequence text) {
        String expected = context.getString(resId);
        assertEqualsString(expected, text);
    }

    public static void assertEqualsString(String expected, CharSequence actual) {
        Assert.assertEquals(expected, actual != null ? actual.toString() : null);
    }

    public static void assertContains(TextView textView, Object... substrings) {
        assertContains(textView.getText(), substrings);
    }

    public static void assertEmpty(TextView textView) {
        String actual = textView.getText().toString();
        Assert.assertTrue("expect empty but actual is '" + actual + "'", TextUtils.isEmpty(actual));
    }

    public static void assertEqualsDrawable(Context context, int expectedResId, Drawable actual) {
        Resources r = context.getResources();
        Drawable expected = expectedResId != View.NO_ID ? r.getDrawable(expectedResId) : null;
        assertEqualsDrawable(actual, expected);
    }

    private static void assertEqualsDrawable(Drawable actual, Drawable expected) {
        if (actual == expected) {
            return;
        }
        Assert.assertTrue(actual != null || expected != null);
        Assert.assertTrue(expected.getConstantState().equals(actual.getConstantState()));
    }

    public static void assertGone(View view) {
        assertVisibility(View.GONE, view);
    }

    public static void assertVisibility(int expected, View view) {
        int visibility = view.getVisibility();
        if (visibility != expected) {
            String msg = "expected " + visibilityToString(expected)
                    + " but view.getVisibility() = " + visibilityToString(visibility)
                    + " for " + viewToString(view);
            fail(msg);
        }
    }

    private static String viewToString(View view) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            return "'" + tv.getText() + "'";
        } else {
            return view.toString();
        }
    }

    private static String visibilityToString(int visibility) {
        switch (visibility) {

        case View.VISIBLE:
            return "VISIBLE";

        case View.INVISIBLE:
            return "INVISIBLE";

        case View.GONE:
            return "GONE";

        default:
            throw new IllegalArgumentException("visibility = " + visibility);
        }
    }

    public static void assertVisible(View view) {
        assertVisibility(View.VISIBLE, view);
    }

    public static void assertEqualsComponentName(Context context, Class<?> cClass, Intent actual) {
        Assert.assertNotNull("actual intent is null", actual);
        Assert.assertEquals(new ComponentName(context, cClass), actual.getComponent());
    }

    public static void assertIntentFlags(Intent intent, int flags) {
        Assert.assertTrue((intent.getFlags() & flags) != 0);
    }

    public static void assertEqualsIntent(Intent expected, Intent actual) {
        if (expected == actual) {
            return;
        }
        Assert.assertNotNull("null intent", actual);
        Assert.assertTrue("expected intent " + expected + ", but actuail " + actual,
                expected.filterEquals(actual));
        Assert.assertTrue("expected intent extras " + expected.getExtras() +
                ",\nbut actual " + actual.getExtras(),
                isBundlesEqual(expected.getExtras(), actual.getExtras()));
    }

    public static boolean isBundlesEqual(Bundle expected, Bundle actual) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null) {
            return false;
        }

        if (actual == null) {
            return false;
        }

        if (expected.keySet().equals(actual.keySet())) {
            for (String key : expected.keySet()) {
                if (!expected.get(key).equals(actual.get(key))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static void assertEqualsBitmap(Context context, int resId, Bitmap actual) {
        BitmapDrawable expected = (BitmapDrawable) context.getResources().getDrawable(resId);
        assertEqualsBitmap(expected.getBitmap(), actual);
    }

    public static void assertEqualsBitmap(Bitmap expected, Bitmap actual) {
        if (!Bitmaps.equals(expected, actual)) {
            String msg = "Bitmaps not equals expected " + firstThreeBytes(expected) + " but " +
                    " actual " + firstThreeBytes(actual);
            throw new AssertionFailedError(msg);
        }
    }

    private static String firstThreeBytes(Bitmap bitmap) {
        if (bitmap != null) {
            byte[] bytes = Bitmaps.bitmapToBytes(bitmap);
            if (bytes.length >= 3) {
                return "[" + bytes[0] + ", " + bytes[1] + ", " + bytes[2] + ",..]";
            } else {
                return bitmap.toString();
            }
        } else {
            return null;
        }
    }

    public static void assertEqualsArray(byte[] expected, byte[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }

    public static void assertStringStartWith(String actual, String expectedStart) {
        if (!actual.startsWith(expectedStart)) {
            throw new AssertionError("expect string start `" + expectedStart
                    + "', but is `" + actual + "'");
        }
    }

    public static void
            assertProgressEquals(double expectedProgress, double expectedMax, ProgressBar bar) {
        assertNotNull(bar);
        assertRationEquals(expectedProgress, expectedMax, bar.getProgress(), bar.getMax());
    }

    private static void assertRationEquals(double eDividend, double eDivisor,
                    double aDividend, double aDivisor) {
        Assert.assertEquals(eDividend / eDivisor, aDividend / aDivisor, 0.001);
    }
}
