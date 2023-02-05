package com.yandex.mail.asserts;

import com.yandex.mail.tools.User;

import org.assertj.core.api.Condition;

import androidx.annotation.NonNull;

public final class FolderConditions {

    private FolderConditions() { }

    @NonNull
    public static Condition<User.LocalFolder> totalCount(int count) {
        return new Condition<User.LocalFolder> () {
            @Override
            public boolean matches(User.LocalFolder folder) {
                return folder.queryCountTotal() == count;
            }
        };
    }

    @NonNull
    public static Condition<User.LocalFolder> unreadCount(int count) {
        return new Condition<User.LocalFolder> () {
            @Override
            public boolean matches(User.LocalFolder folder) {
                return folder.queryCountUnread() == count;
            }
        };
    }

    @NonNull
    public static Condition<User.LocalFolder> empty() {
        return totalCount(0);
    }
}
