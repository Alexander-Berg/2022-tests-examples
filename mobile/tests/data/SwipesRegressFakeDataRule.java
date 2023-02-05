package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.wrappers.AttachmentWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import androidx.annotation.NonNull;

import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;

public class SwipesRegressFakeDataRule extends FakeAccountRule {

    public static final String USER_FOLDER_FOR_SWIPE = "Swipe";

    public static final String THREAD_FOR_SWIPE_IMPORTANT = "Important thread";

    public static final String THREAD_FOR_SWIPE = "Thread for swipe";

    public static final String MESSAGE_TO_ARCHIVE = "Message to archive";

    public static final String FIRST_MESSAGE_WITH_ATTACH = "It's a message with nice attachment";

    public static final String SECOND_MESSAGE_WITH_ATTACH = "It's another message with nice attachment";

    public static final String ATTACH_NAME = "Thinking In Java 4th.pdf";

    public SwipesRegressFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        AttachmentWrapper attachment = AttachmentWrapper.newTextAttachment(ATTACH_NAME, "");

        FolderWrapper userFolder = account.newFolder(USER_FOLDER_FOR_SWIPE).build();
        FolderWrapper folderToMoveIn = account.newFolder(USERS_FOLDER_MEDUZA).build();
        account.addFolders(userFolder, folderToMoveIn);

        LabelWrapper userLabel = account.newLabel(LABEL_NAME).build();
        account.addLabels(userLabel);

        account.addThreads(
                account.newThread(
                        account.newUnreadMessage(userFolder).labels(account.getImportantLabel()).subjText(THREAD_FOR_SWIPE_IMPORTANT),
                        account.newUnreadMessage(account.getInboxFolder()).subjText(THREAD_FOR_SWIPE_IMPORTANT),
                        account.newUnreadMessage(account.getSentFolder()).subjText(THREAD_FOR_SWIPE_IMPORTANT)
                ).build()
        );

        account.addThreads(
                account.newThread(
                        account.newUnreadMessage(userFolder).subjText(THREAD_FOR_SWIPE),
                        account.newUnreadMessage(account.getInboxFolder()).subjText(THREAD_FOR_SWIPE),
                        account.newUnreadMessage(account.getSentFolder()).subjText(THREAD_FOR_SWIPE)
                ).build()
        );

        account.addMessages(
                account.newReadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_TO_ARCHIVE)
                        .build(),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(FIRST_MESSAGE_WITH_ATTACH)
                        .attachment(attachment)
                        .build(),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(SECOND_MESSAGE_WITH_ATTACH)
                        .attachment(attachment)
                        .build()
        );
    }
}
