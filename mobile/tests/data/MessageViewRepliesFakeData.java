package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.wrappers.AttachmentWrapper;

import androidx.annotation.NonNull;

import static com.yandex.mail.pages.MessageViewPage.NAME_ATTACH_FROM_FORWARDING_MESSAGE;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGE_FORWARD_WITH_ATTACH;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGE_REPLY_FORWARD;
import static kotlin.collections.CollectionsKt.listOf;

public class MessageViewRepliesFakeData extends FakeAccountRule {

    public MessageViewRepliesFakeData(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        AttachmentWrapper file = AttachmentWrapper.newTextAttachment(NAME_ATTACH_FROM_FORWARDING_MESSAGE, "");

        account.addMessages(
                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_REPLY_FORWARD)
                        .build(),

                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_FORWARD_WITH_ATTACH)
                        .attachments(listOf(file))
                        .build()
        );
    }
}
