package ru.yandex.partner.core.entity.queue;

import javax.annotation.Nonnull;

import ru.yandex.partner.libs.i18n.GettextMsg;
import ru.yandex.partner.libs.i18n.GettextMsgPayload;
import ru.yandex.partner.libs.i18n.GettextSyncExclude;

import static ru.yandex.partner.libs.i18n.GettextMsgPayload.Builder.msg;

public enum TestTaskTypeMsg implements GettextMsg, GettextSyncExclude {
    REGULAR_TASK(msg("Regular task")),
    NON_CONCURRENT_TASK(msg("Non-concurrent task"));

    private static final String KEYSET_NAME = "test_messages";
    private final GettextMsgPayload payload;

    TestTaskTypeMsg(GettextMsgPayload.Builder builder) {
        this.payload = builder.build();
    }

    @Nonnull
    @Override
    public GettextMsgPayload getPayload() {
        return payload;
    }

    @Nonnull
    @Override
    public String getKeysetName() {
        return KEYSET_NAME;
    }

}
