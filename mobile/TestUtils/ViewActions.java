package ru.yandex.direct.ui.testutils;

import android.graphics.Rect;
import com.google.android.material.appbar.AppBarLayout;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.core.widget.NestedScrollView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.Spinner;

import org.hamcrest.Matcher;

import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;

public final class ViewActions {
    private ViewActions() { }

    public static ViewAction setSpinnerSelection(final int position) {
        return new ViewAction() {
            @Override
            public void perform(UiController uiController, View view) {
                Spinner spinner = (Spinner) view;
                spinner.setSelection(position);
                uiController.loopMainThreadUntilIdle();
            }

            @Override
            public String getDescription() {
                return "Set a selected item in a Spinner";
            }

            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(Spinner.class);
            }
        };
    }

    public static ViewAction collapse() {
        return setExpanded(false);
    }

    public static ViewAction expand() {
        return setExpanded(true);
    }

    private static ViewAction setExpanded(final boolean expanded) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(AppBarLayout.class);
            }

            @Override
            public String getDescription() {
                return (expanded ? "Expand" : "Collapse") + " AppBarLayout";
            }

            @Override
            public void perform(UiController uiController, View view) {
                AppBarLayout toolbarLayout = ((AppBarLayout) view);
                toolbarLayout.setExpanded(expanded, false);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setChecked(final boolean isChecked) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(CompoundButton.class);
            }

            @Override
            public String getDescription() {
                return "Set CheckBox state to: " + (isChecked ? "checked" : "unchecked");
            }

            @Override
            public void perform(UiController uiController, View view) {
                CompoundButton checkBox = (CompoundButton) view;
                checkBox.setChecked(isChecked);
            }
        };
    }

    public static ViewAction nestedScrollTo() {
        return actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(
                        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                        isDescendantOfA(anyOf(
                                isAssignableFrom(ScrollView.class),
                                isAssignableFrom(HorizontalScrollView.class),
                                isAssignableFrom(NestedScrollView.class)
                        ))
                );
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (isDisplayingAtLeast(90).matches(view)) {
                    return;
                }
                Rect rect = new Rect();
                view.getDrawingRect(rect);
                view.requestRectangleOnScreen(rect, true);
                uiController.loopMainThreadUntilIdle();
            }

            @Override
            public String getDescription() {
                return "scroll to";
            }
        });
    }
}
