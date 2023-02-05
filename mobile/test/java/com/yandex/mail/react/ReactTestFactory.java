package com.yandex.mail.react;

import com.yandex.mail.react.entity.ReactMessage;
import com.yandex.mail.react.entity.ReactTranslator;
import com.yandex.mail.react.entity.ThreadMeta;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;

import static kotlin.collections.CollectionsKt.listOf;

public final class ReactTestFactory {

    private ReactTestFactory() {
        throw new IllegalStateException("No instances please");
    }

    @NonNull
    public static ReactMessage buildMessageWithIdWithoutBody(long messageId) {
        List<ReactMessage.Action> actions = listOf(
                ReactMessage.Action.REPLY,
                ReactMessage.Action.COMPOSE,
                ReactMessage.Action.REPLY_ALL
        );
        return new ReactMessage.Builder()
                .messageId(messageId)
                .typeMask(4096)
                .folderId(10L)
                .typeMask(4096)
                .time(new Date().toString())
                .timestamp(System.currentTimeMillis())
                .firstLine("Yo")
                .translator(new ReactTranslator(
                        "sourceLang",
                        "targetLang",
                        "",
                        "submitButtonText",
                        "translateFromText",
                        "translateToText",
                        false,
                        false,
                        false,
                        false,
                        false
                ))
                .controls(actions)
                .read(true)
                .build();
    }

    @NonNull
    public static ReactMessage buildMessageWithIdWithBody(long messageId, @NonNull String body) {
        List<ReactMessage.Action> actions = listOf(
                ReactMessage.Action.REPLY,
                ReactMessage.Action.COMPOSE,
                ReactMessage.Action.REPLY_ALL
        );
        return new ReactMessage.Builder()
                .messageId(messageId)
                .typeMask(4096)
                .folderId(10L)
                .typeMask(4096)
                .time(new Date().toString())
                .timestamp(System.currentTimeMillis())
                .firstLine("Yo")
                .translator(new ReactTranslator(
                        "sourceLang",
                        "targetLang",
                        "",
                        "submitButtonText",
                        "translateFromText",
                        "translateToText",
                        false,
                        false,
                        false,
                        false,
                        false
                ))
                .controls(actions)
                .read(true)
                .body(body)
                .build();
    }

    @NonNull
    public static ReactMessage buildDraftWithIdWithoutBody(long messageId) {
        return new ReactMessage.Builder()
                .messageId(messageId)
                .folderId(10L)
                .draft(true)
                .typeMask(4096)
                .time(new Date().toString())
                .timestamp(System.currentTimeMillis())
                .firstLine("Yo")
                .read(true)
                .build();
    }

    @NonNull
    public static ReactMessage buildMessageWithMultipleParticipants(long messageId) {
        List<ReactMessage.Action> actions = listOf(
                ReactMessage.Action.REPLY,
                ReactMessage.Action.COMPOSE,
                ReactMessage.Action.REPLY_ALL
        );
        return new ReactMessage.Builder()
                .messageId(messageId)
                .folderId(10L)
                .typeMask(4096)
                .time(new Date().toString())
                .timestamp(System.currentTimeMillis())
                .firstLine("Yo")
                .translator(new ReactTranslator(
                        "sourceLang",
                        "targetLang",
                        "",
                        "submitButtonText",
                        "translateFromText",
                        "translateToText",
                        false,
                        false,
                        false,
                        false,
                        false
                ))
                .controls(actions)
                .participantsCount(2)
                .read(true)
                .build();
    }


    @NonNull
    public static List<ReactMessage> buildListOfMessagesWithMultipleParticipants(int count) {
        final List<ReactMessage> messages = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            messages.add(buildMessageWithMultipleParticipants(i));
        }

        return messages;
    }

    @NonNull
    public static List<ReactMessage> buildListOfMessagesWithoutBodies(int count) {
        final List<ReactMessage> messages = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            messages.add(buildMessageWithIdWithoutBody(i));
        }

        return messages;
    }

    @NonNull
    public static List<ReactMessage> buildListOfMessagesWithBodies(int count, @NonNull String bodyPrefix) {
        final List<ReactMessage> messages = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            messages.add(buildMessageWithIdWithBody(i, bodyPrefix + i));
        }

        return messages;
    }

    @NonNull
    public static List<ReactMessage> buildListOfMessagesWithDraft(int count) {
        final List<ReactMessage> messages = new ArrayList<>(count);

        messages.add(buildDraftWithIdWithoutBody(0));

        for (int i = 1; i < count; i++) {
            messages.add(buildMessageWithIdWithoutBody(i));
        }

        return messages;
    }

    @NonNull
    static ThreadMeta buildThreadMeta() {
        return threadMetaBuilder().build();
    }

    @NonNull
    static ThreadMeta.Builder threadMetaBuilder() {
        return new ThreadMeta.Builder(
                "Some subject",
                3,
                1,
                0,
                true,
                false
        );
    }
}
