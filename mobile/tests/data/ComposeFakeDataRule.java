package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;

import androidx.annotation.NonNull;

public class ComposeFakeDataRule extends FakeAccountRule {

    public static final String MESSAGE_REPLY_FORWARD_DRAFT = "MessageReplyForwardSaveDraft";

    public ComposeFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addMessages(
                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_REPLY_FORWARD_DRAFT)
                        .build()
        );
    }

}
