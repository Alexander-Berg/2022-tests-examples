package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.storage.MessageStatus;
import com.yandex.mail.wrappers.AttachmentWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.TestUtil.convertStringToDate;
import static com.yandex.mail.pages.MessageViewPage.ADDRESS_RECIPIENT_TO;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static kotlin.collections.CollectionsKt.map;

public class ViewActionsInSearchFakeDataRule extends FakeAccountRule {

    public static final String SOCIAL_MESSAGE1_IN_INBOX = "social_filter_message1";

    public static final String SOCIAL_MESSAGE2_IN_INBOX = "social_filter_message2";

    public static final String VIEW_OPERATION_IN_SEARCH_MESSAGE = "view_operation_in_search_view";

    public static final String IMPORTANT_MESSAGE_IN_SEARCH = "important_message_in_search_view";

    public static final String MESSAGE_IN_USER_FOLDER = "message_in_user_folder_search_view";

    public static final String MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE = "message_with_attach_in_search_firstline";

    public static final String MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ = "message_with_attach_in_search";

    public static final String ATTACH_NAME_FOR_MESSAGE_IN_SEARCH = "attach_in_search";

    public static final String SPAM_MESSAGE_SUBJECT = "Spam message for view action in search";

    public ViewActionsInSearchFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addArchiveFolder();

        LabelWrapper userLabel = account.newLabel(LABEL_NAME).build();
        account.addLabels(userLabel);

        FolderWrapper userFolder = account.newFolder(USERS_FOLDER_MEDUZA).build();
        account.addFolders(userFolder);

        AttachmentWrapper attachment = AttachmentWrapper.newTextAttachment(ATTACH_NAME_FOR_MESSAGE_IN_SEARCH, "");

        final List<String> subjects = CollectionsKt.listOf(SOCIAL_MESSAGE1_IN_INBOX, SOCIAL_MESSAGE2_IN_INBOX);
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
                account.newReadMessage(userFolder)
                        .subjText(VIEW_OPERATION_IN_SEARCH_MESSAGE)
                        .build(),
                account.newReadMessage(account.getInboxFolder())
                        .subjText(IMPORTANT_MESSAGE_IN_SEARCH)
                        .labels(account.getImportantLabel())
                        .build(),
                account.newReadMessage(userFolder)
                        .subjText(MESSAGE_IN_USER_FOLDER)
                        .build(),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_WITH_ATTACH_IN_SEARCH_SUBJ)
                        .content(MESSAGE_WITH_ATTACH_IN_SEARCH_FIRSTLINE)
                        .timestamp(convertStringToDate(MESSAGE_DATE_TEXT))
                        .to(ADDRESS_RECIPIENT_TO)
                        .attachment(attachment)
                        .labels(userLabel, account.getImportantLabel())
                        .build(),
                account.newReadMessage(account.getSpamFolder())
                        .subjText(SPAM_MESSAGE_SUBJECT)
                        .build()
        );
    }
}
