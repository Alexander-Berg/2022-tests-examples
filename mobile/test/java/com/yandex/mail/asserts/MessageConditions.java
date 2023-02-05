package com.yandex.mail.asserts;

import android.annotation.SuppressLint;

import com.yandex.mail.tools.User;

import org.assertj.core.api.Condition;
import org.assertj.core.description.Description;
import org.assertj.core.description.TextDescription;

import androidx.annotation.NonNull;

@SuppressLint("NewApi")
public class MessageConditions {

    private MessageConditions() { }

    @NonNull
    public static Condition<User.LocalMessage> inFolder(@NonNull User.LocalFolder folder) {
        return new Condition<User.LocalMessage> () {
            @Override
            public boolean matches(User.LocalMessage message) {
                return message.getLocalFid() == folder.folderId;
            }
        };
    }

    @NonNull
    public static Condition<User.LocalMessage> inFolder(long folderId) {
        return new Condition<User.LocalMessage> () {
            @Override
            public boolean matches(User.LocalMessage message) {
                return Long.parseLong(message.getServerFid()) == folderId;
            }
        };
    }

    /**
     * Indicates if message is marked with this label
     */
    @NonNull
    public static Condition<User.LocalMessage> label(@NonNull User.LocalLabel label) {
        return new Condition<User.LocalMessage>() {
            @Override
            public boolean matches(@NonNull User.LocalMessage message) {
                return message.getServerLids().contains(label.getServerLid());
            }

            // TODO ???
            @Override
            public Description description() {
                return new TextDescription("label %d", label.labelId);
            }
        };
    }
}
