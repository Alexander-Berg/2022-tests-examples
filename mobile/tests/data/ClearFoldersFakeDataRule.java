package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;

import androidx.annotation.NonNull;

import static java.lang.String.format;
import static kotlin.collections.CollectionsKt.map;
import static kotlin.ranges.RangesKt.until;

public class ClearFoldersFakeDataRule extends FakeAccountRule {

    public final static int MESSAGE_COUNT = 30;

    public ClearFoldersFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addMessages(
                map(until(0, MESSAGE_COUNT),
                        i -> account
                                .newUnreadMessage(account.getSpamFolder())
                                .subjText(format("Spam message %d", i)).build()
                )
        );

        account.addMessages(
                map(until(0, MESSAGE_COUNT),
                        i -> account
                                .newUnreadMessage(account.getTrashFolder())
                                .subjText(format("Trash message %d", i)).build()
                )
        );
        // add a message to Inbox, because another toast is shown instead of "Folder in empty" when Inbox is empty.
        // it is not a bug, it is a feature
        account.addMessages(
                account.newReadMessage(account.getInboxFolder())
                        .subjText("Inbox message")
                        .build()
        );
    }

}
