package com.yandex.mail.asserts;

import com.yandex.mail.tools.User;

import org.assertj.core.api.Condition;

import androidx.annotation.NonNull;

public class LabelConditions {

    private LabelConditions() { }

    @NonNull
    public static Condition<User.LocalLabel> totalCount(int count) {
        return new Condition<User.LocalLabel> () {
            @Override
            public boolean matches(User.LocalLabel label) {
                return label.queryCountTotal() == count;
            }
        };
    }
}
