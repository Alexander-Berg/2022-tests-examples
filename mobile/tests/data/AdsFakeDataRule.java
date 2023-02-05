package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;

import androidx.annotation.NonNull;

import static kotlin.collections.CollectionsKt.map;
import static kotlin.ranges.RangesKt.until;

public class AdsFakeDataRule extends FakeAccountRule {

    public static final String TEST_FAKE_ADS_MESSAGE = "Fake ads message";

    public static final int MESSAGES_COUNT_IN_ADS_FOLDER = 20;

    public AdsFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addMessages(
                map(until(0, MESSAGES_COUNT_IN_ADS_FOLDER),
                        i -> account
                                .newUnreadMessage(account.getInboxFolder())
                                .subjText(TEST_FAKE_ADS_MESSAGE + i).build()
                )
        );
    }
}
