package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.wrappers.AttachmentWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;
import com.yandex.mail.wrappers.ThreadWrapper;

import androidx.annotation.NonNull;

import static com.yandex.mail.TestUtil.convertStringToDate;
import static com.yandex.mail.pages.MessageViewPage.ADDRESS_RECIPIENT_CC;
import static com.yandex.mail.pages.MessageViewPage.ADDRESS_RECIPIENT_TO;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_SUBJECT_TEXT;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_VIEW_FIRSTLINE_TEXT;
import static com.yandex.mail.pages.MessageViewPage.NAME_ATTACH_FROM_FORWARDING_MESSAGE;
import static com.yandex.mail.pages.MessageViewPage.THREAD_WITH_DRAFTS_SUBJECT;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.OperationsConst.ForMessageView.MESSAGE_WITH_LABELS_AND_RECIPIENTS;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.FOLDER_FOR_SWITCHING_THREADS;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.MESSAGE_FIRST;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.MESSAGE_LAST;
import static com.yandex.mail.util.OperationsConst.ForSwitchThreads.MESSAGE_MIDDLE;

public class MessageViewFakeData extends FakeAccountRule {

    public MessageViewFakeData(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        AttachmentWrapper attachment = AttachmentWrapper.newTextAttachment(NAME_ATTACH_FROM_FORWARDING_MESSAGE, "");

        LabelWrapper userLabel = account.newLabel(LABEL_NAME).build();
        account.addLabels(userLabel);

        FolderWrapper userFolder = account.newFolder(FOLDER_FOR_SWITCHING_THREADS).build();
        account.addFolders(userFolder);

        account.addMessages(
                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_SUBJECT_TEXT)
                        .content(MESSAGE_VIEW_FIRSTLINE_TEXT)
                        .timestamp(convertStringToDate(MESSAGE_DATE_TEXT))
                        .attachment(attachment)
                        .to(ADDRESS_RECIPIENT_TO)
                        .cc(ADDRESS_RECIPIENT_CC)
                        .labels(userLabel, account.getImportantLabel())
                        .build(),

                account.newReadMessage(userFolder)
                        .subjText(MESSAGE_LAST)
                        .build(),

                account.newReadMessage(userFolder)
                        .subjText(MESSAGE_MIDDLE)
                        .build(),

                account.newReadMessage(userFolder)
                        .subjText(MESSAGE_FIRST)
                        .build(),

                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_WITH_LABELS_AND_RECIPIENTS)
                        .to(ADDRESS_RECIPIENT_TO)
                        .cc(ADDRESS_RECIPIENT_CC)
                        .labels(userLabel, account.getImportantLabel())
                        .build()
        );

        ThreadWrapper threadWrapper = account.newThread(
                account.newReadMessage(account.getInboxFolder())
                        .subjText(THREAD_WITH_DRAFTS_SUBJECT),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(THREAD_WITH_DRAFTS_SUBJECT),
                account.newUnreadMessage(account.getDraftFolder())
                        .subjText(THREAD_WITH_DRAFTS_SUBJECT)

        ).build();

        account.addThreads(threadWrapper);
    }
}
