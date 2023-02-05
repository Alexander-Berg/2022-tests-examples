package com.yandex.mail.tests.data;

import com.yandex.mail.TestUtil;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import androidx.annotation.NonNull;

import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static kotlin.collections.CollectionsKt.map;
import static kotlin.ranges.RangesKt.until;

public class SearchGroupOperationsFakeDataRule extends FakeAccountRule {

    public static final String MESSAGE_IN_USER_FOLDER = "message_in_user_folder_search_go";

    public static final String MESSAGE_IN_INBOX = "message_in_inbox_go";

    public static final String SWIPE_AFTER_SCROLL_THREAD = "swipe_after_scroll_thread";

    public static int MESSAGE_COUNT = 10;

    public SearchGroupOperationsFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {

        account.addArchiveFolder();

        LabelWrapper userLabel = account.newLabel(LABEL_NAME).build();
        account.addLabels(userLabel);

        FolderWrapper userFolder = account.newFolder(USERS_FOLDER_MEDUZA).build();
        account.addFolders(userFolder);

        account.addMessages(
                account.newReadMessage(userFolder)
                        .subjText(MESSAGE_IN_USER_FOLDER)
                        .build(),
                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_IN_INBOX)
                        .build()
        );
        account.addThreads(
                account.newThread(
                        map(until(0, MESSAGE_COUNT),
                                i -> account
                                        .newReadMessage(userFolder)
                                        .subjText(SWIPE_AFTER_SCROLL_THREAD)
                                        .content(String.format(SWIPE_AFTER_SCROLL_THREAD + "%d", i))
                                        .timestamp(TestUtil.convertStringToDate(MESSAGE_DATE_TEXT + i))
                        )
                ).build()
        );
    }
}


