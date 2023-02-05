package com.yandex.mail.yables;

import android.view.View;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;

import java.util.List;

public class YableReflowViewAssert extends AbstractAssert<YableReflowViewAssert, YableReflowView> {
    protected YableReflowViewAssert(YableReflowView actual) {
        this(actual, YableReflowViewAssert.class);
    }

    protected YableReflowViewAssert(YableReflowView actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public static YableReflowViewAssert assertThat(YableReflowView wrapper) {
        return new YableReflowViewAssert(wrapper);
    }

    public YableReflowViewAssert hasYable(String address) {
        isNotNull();

        Assertions.assertThat(actual.getChildYables()).haveAtLeastOne(new Condition<YableView>() {
            @Override
            public boolean matches(YableView value) {
                return value.getRecipientText().contains(address);
            }
        });
        return this;
    }

    /**
     * Reflow view is expanded if all its child yables are visible
     */
    public YableReflowViewAssert isExpanded() {
        isNotNull();

        Assertions.assertThat(actual.getChildYables()).are(new Condition<YableView>() {
            @Override
            public boolean matches(YableView value) {
                return value.getVisibility() == View.VISIBLE;
            }
        });
        return this;
    }

    /**
     * Reflow view is collapsed if all its child yables but the first are invisible.
     * Note that if the reflow contains only one yable, it is simultaneously collapsed and expanded,
     * but anyway its internal state is not observable in such case
     */
    public YableReflowViewAssert isCollapsed() {
        isNotNull();

        List<YableView> yables = actual.getChildYables();
        if (yables.size() >= 1) {
            Assertions.assertThat(yables.get(0).getVisibility()).isEqualTo(View.VISIBLE);
        }

        if (yables.size() >= 2) {
            Assertions.assertThat(yables.subList(1, yables.size())).are(new Condition<YableView>() {
                @Override
                public boolean matches(YableView value) {
                    return value.getVisibility() == View.GONE;
                }
            });
        }
        return this;
    }
}