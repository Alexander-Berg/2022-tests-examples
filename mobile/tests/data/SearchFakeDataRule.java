package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.storage.MessageStatus;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import java.util.List;

import androidx.annotation.NonNull;

import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static kotlin.collections.CollectionsKt.listOf;
import static kotlin.collections.CollectionsKt.map;

public class SearchFakeDataRule extends FakeAccountRule {

    public static final String SOCIAL_MESSAGE1_IN_INBOX = "social_filter_message1";

    public static final String SOCIAL_MESSAGE2_IN_INBOX = "social_filter_message2";

    public static final String SEARCH_IN_CURRENT_FOLDER_MESSAGE = "test_search_current_folder";

    public SearchFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {

        account.addArchiveFolder();
        account.addTemplateFolder();

        LabelWrapper userLabel = account.newLabel(LABEL_NAME).build();
        account.addLabels(userLabel);

        FolderWrapper userFolder = account.newFolder(USERS_FOLDER_MEDUZA).build();

        account.addFolders(userFolder);

        final List<String> subjects = listOf(SOCIAL_MESSAGE1_IN_INBOX, SOCIAL_MESSAGE2_IN_INBOX);
        account.addMessages(
                map(subjects,
                                subject -> account
                                        .newReadMessage(account.getInboxFolder())
                                        .subjText(subject)
                                        .types(MessageStatus.Type.S_SOCIAL)
                                        .build()
                        )
        );
        account.addMessages(
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(account.getSentFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(userFolder)
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(account.getSpamFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(account.getTrashFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(account.getArchiveFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(account.getDraftFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build(),
                account.newUnreadMessage(account.getTemplateFolder())
                        .subjText(SEARCH_IN_CURRENT_FOLDER_MESSAGE)
                        .build()
        );
    }
}
