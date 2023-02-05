package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;

import androidx.annotation.NonNull;

import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_INBOX;
import static com.yandex.mail.util.OperationsConst.ForMessageView.FAKE_TEST_MESSAGE_IN_SPAM;
import static com.yandex.mail.util.OperationsConst.ForMessageView.SECOND_FAKE_MESSAGE_IN_INBOX;
import static com.yandex.mail.util.OperationsConst.ForMessageView.SECOND_FAKE_MESSAGE_IN_SPAM;

public class MessageViewActionsFakeDataRule extends FakeAccountRule {

    public MessageViewActionsFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addMessages(
                account.newReadMessage(account.getInboxFolder())
                        .subjText(FAKE_TEST_MESSAGE_IN_INBOX)
                        .build(),

                account.newReadMessage(account.getInboxFolder())
                        .subjText(SECOND_FAKE_MESSAGE_IN_INBOX)
                        .build(),

                account.newReadMessage(account.getSpamFolder())
                        .subjText(FAKE_TEST_MESSAGE_IN_SPAM)
                        .build(),

                account.newReadMessage(account.getSpamFolder())
                        .subjText(SECOND_FAKE_MESSAGE_IN_SPAM)
                        .build()
        );

        account.addFolders(account.newFolder(USERS_FOLDER_MEDUZA).build());
    }
}
