package com.yandex.mail.tools;

import android.content.Intent;

import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Proxies Robolectric {@link SupportFragmentController}.
 * Ideally, we should just use {@link SupportFragmentController}, but the changes to Robolectric might propagate slowly, and we want to move fast.
 * Once the corresponding change made it to Robolectric, we should remove the corresponding code from this class.
 */
public class FragmentController<T extends Fragment> extends SupportFragmentController<T> {

    public FragmentController(
            @NonNull T fragment,
            @NonNull Class<? extends FragmentActivity> activityClass
    ) {
        super(fragment, activityClass);
    }

    public FragmentController(
            @NonNull T fragment,
            @NonNull Class<? extends FragmentActivity> activityClass,
            @Nullable Intent intent
    ) {
        super(fragment, activityClass, intent);
    }

    @NonNull
    public ActivityController getActivityController() {
        return ReflectionHelpers.getField(this, "activityController");
    }

    @NonNull
    public static <F extends Fragment> FragmentController<F> of(
            @NonNull F fragment,
            @NonNull Class<? extends FragmentActivity> activityClass,
            @Nullable Intent intent
    ) {
        return new FragmentController<>(fragment, activityClass, intent);
    }

    @NonNull
    public static <F extends Fragment> FragmentController<F> of(@NonNull F fragment, @NonNull Class<? extends FragmentActivity> activityClass) {
        return new FragmentController<>(fragment, activityClass);
    }

    @NonNull
    public static <F extends Fragment> FragmentController<F> of(@NonNull F fragment) {
        return of(fragment, TestFragmentActivity.class);
    }
}
