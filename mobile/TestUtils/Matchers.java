package ru.yandex.direct.ui.testutils;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Matchers {

    private static final String WHITESPACES = "[ \\t\\xA0\\u1680\\u180e\\u2000-\\u200a\\u202f\\u205f\\u3000]";

    private Matchers() {
    }

    public static Matcher<View> atPositionOfRecyclerView(final int recyclerViewId, final int position, final int... targetViewIds) {

        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            List<View> children = new ArrayList<>();

            @Override
            public void describeTo(Description description) {
                String recyclerViewIdDescription;
                String targetViewsIdDescription;

                if (resources != null) {
                    recyclerViewIdDescription = resources.getResourceName(recyclerViewId);
                    String[] idDescriptions = new String[targetViewIds.length];
                    for (int i = 0; i < targetViewIds.length; i++) {
                        idDescriptions[i] = resources.getResourceName(targetViewIds[i]);
                    }
                    targetViewsIdDescription = Arrays.toString(idDescriptions);
                } else {
                    recyclerViewIdDescription = String.valueOf(recyclerViewId);
                    targetViewsIdDescription = Arrays.toString(targetViewIds);
                }

                description.appendText(String.format("in position %s of recycleView %s with chain of ids: %s", position, recyclerViewIdDescription, targetViewsIdDescription));
            }

            @Override
            public boolean matchesSafely(View view) {
                this.resources = view.getResources();

                if (recyclerViewId == view.getId()) {
                    RecyclerView recyclerView = (RecyclerView) view;
                    ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                    if (viewHolder == null) {
                        return false;
                    }

                    View childView = viewHolder.itemView;
                    for (int i = 0; childView != null && i < targetViewIds.length; i++) {
                        childView = childView.findViewById(targetViewIds[i]);
                    }

                    if (childView != null) {
                        children.add(childView);
                    }

                    return false;
                }

                for (View child : children) {
                    if (child == view) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                       && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    public static Matcher<View> hasChildCount(final int childCount) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return item instanceof ViewGroup && ((ViewGroup) item).getChildCount() == childCount;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ViewGroup with exactly " + childCount + " children.");
            }
        };
    }

    public static Matcher<View> hasItemsCount(final int itemsCount) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return item instanceof RecyclerView && ((RecyclerView) item).getAdapter().getItemCount() == itemsCount;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with exactly " + itemsCount + " items.");
            }
        };
    }

    public static Matcher<String> asFloat(final Matcher<Float> floatMatcher) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                float parsedValue;

                try {
                    parsedValue = Float.parseFloat(item.replaceAll(WHITESPACES, "").replaceAll(",", "."));
                } catch (NumberFormatException ignored) {
                    return false;
                }

                return floatMatcher.matches(parsedValue);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("that can be parsed as float: ");
                floatMatcher.describeTo(description);
            }
        };
    }

    public static <T> Matcher<T> first(final Matcher<T> matcher) {
        return new TypeSafeMatcher<T>() {
            private boolean isMatch = false;

            @Override
            protected boolean matchesSafely(T item) {
                return !isMatch && (isMatch = matcher.matches(item));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("first item that matches: ");
                matcher.describeTo(description);
            }
        };
    }

    public static Matcher<View> withDrawable(final int drawableResId) {
        if (drawableResId < 0)
            throw new IllegalArgumentException("Expected drawableResId to be not less than zero, but got " +
                drawableResId + ".");

        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                if (!(item instanceof ImageView)) return false;
                Bitmap actual = createBitmap(((ImageView) item).getDrawable());
                Bitmap expected = createBitmap(item.getContext().getResources().getDrawable(drawableResId));
                return expected.sameAs(actual);
            }

            private Bitmap createBitmap(Drawable drawable) {
                Bitmap bitmap = getEmptyBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            }

            private Bitmap getEmptyBitmap(int width, int height) {
                return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("with drawable with resource id: ")
                        .appendText("[" + drawableResId + "]");
            }
        };
    }
}
