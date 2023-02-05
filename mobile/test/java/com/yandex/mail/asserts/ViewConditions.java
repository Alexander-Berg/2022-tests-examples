package com.yandex.mail.asserts;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.assertj.core.api.Condition;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.yandex.mail.asserts.Conditions.matching;

public class ViewConditions {

    private ViewConditions() { }

    @NonNull
    public static Condition<View> shown() {
        return new Condition<View>() {
            @Override
            public boolean matches(@NonNull View view) {
                return view.isShown();
            }
        };
    }

    @NonNull
    public static Condition<View> visible() {
        return matching(view -> view.getVisibility() == VISIBLE);
    }

    @NonNull
    public static Condition<View> gone() {
        return matching(view -> view.getVisibility() == GONE);
    }

    public static Condition<View> focused() {
        return matching(View::hasFocus);
    }

    @NonNull
    public static Condition<Button> enabled() {
        return new Condition<Button>() {
            @Override
            public boolean matches(@NonNull Button button) {
                return button.isEnabled();
            }
        };
    }

    @NonNull
    public static Condition<Button> disabled() {
        return new Condition<Button>() {
            @Override
            public boolean matches(@NonNull Button button) {
                return !button.isEnabled();
            }
        };
    }

    @NonNull
    public static Condition<ViewGroup> emptyHierarchy() {
        return hierarchySize(0);
    }

    @NonNull
    public static Condition<TextView> withText(@StringRes int textRes) {
        return new Condition<TextView>() {
            @Override
            public boolean matches(@NonNull TextView value) {
                final String text = value.getContext().getString(textRes);
                return TextUtils.equals(value.getText(), text);
            }
        };
    }

    @NonNull
    public static Condition<TextView> withText(@NonNull CharSequence text) {
        return new Condition<TextView>() {
            @Override
            public boolean matches(@NonNull TextView value) {
                return TextUtils.equals(value.getText(), text);
            }
        };
    }

    @NonNull
    public static Condition<ViewGroup> hierarchySize(int size) {
        return new Condition<ViewGroup>() {
            @Override
            public boolean matches(@NonNull ViewGroup view) {
                return view.getChildCount() == size;
            }
        };
    }

    @NonNull
    public static Condition<View> onPosition(int left, int top, int right, int bottom) {
        return new Condition<View>() {
            @Override
            public boolean matches(@NonNull View view) {
                return view.getLeft() == left && view.getTop() == top && view.getRight() == right && view.getBottom() == bottom;
            }
        };
    }

    @NonNull
    public static Condition<View> width(int width) {
        return new Condition<View>() {
            @Override
            public boolean matches(@NonNull View view) {
                return view.getWidth() == width;
            }
        };
    }

    @NonNull
    public static Condition<View> measuredSize(int width, int height) {
        return new Condition<View>() {
            @Override
            public boolean matches(@NonNull View view) {
                return view.getMeasuredWidth() == width && view.getMeasuredHeight() == height;
            }
        };
    }

    @NonNull
    public static Condition<View> snackbarWithText(@NonNull ViewGroup parent, @StringRes int resId) {
        return new Condition<View>() {
            @Override
            public boolean matches(@NonNull View view) {
                String text = view.getContext().getString(resId);
                return snackbarWithText(parent, text).matches(view);
            }
        };
    }

    @NonNull
    public static Condition<View> snackbarWithText(@NonNull ViewGroup parent, @NonNull CharSequence text) {
        return new Condition<View>() {
            @Override
            public boolean matches(@NonNull View view) {
                Snackbar.SnackbarLayout snackbarLayout = searchForSnackbarRecursive(parent);
                if (snackbarLayout != null) {
                    TextView snackbarTextUi = snackbarLayout.findViewById(com.google.android.material.R.id.snackbar_text);
                    return TextUtils.equals(text, snackbarTextUi.getText());
                }

                return false;
            }
        };
    }

    public static Snackbar.SnackbarLayout searchForSnackbarRecursive(ViewGroup parent) {
        final int count = parent.getChildCount();
        Snackbar.SnackbarLayout snackbarLayout;
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            if (parent.getChildAt(i) instanceof Snackbar.SnackbarLayout) {
                return (Snackbar.SnackbarLayout) child;
            } else if (child instanceof ViewGroup) {
                snackbarLayout = searchForSnackbarRecursive((ViewGroup) child);
                if (snackbarLayout != null) {
                    return snackbarLayout;
                }
            }
        }
        return null;
    }
}
