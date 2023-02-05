package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.wrappers.AttachmentWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import java.util.List;

import androidx.annotation.NonNull;

import static com.yandex.mail.TestUtil.convertStringToDate;
import static com.yandex.mail.pages.MessageViewPage.ADDRESS_RECIPIENT_TO;
import static com.yandex.mail.pages.MessageViewPage.MESSAGE_DATE_TEXT;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static kotlin.collections.CollectionsKt.listOf;
import static kotlin.collections.CollectionsKt.map;
import static kotlin.ranges.RangesKt.until;

public class MessageListFakeDataRule extends FakeAccountRule {

    public static final int MESSAGE_COUNT = 3;

    public static final String MOVE_TO_SPAM_SINGLE_MESSAGE = "group_mode_spam_firstline1";

    public static final String DELETE_SINGLE_MESSAGE = "group_mode_delete_message2";

    public static final String ARCHIVE_SINGLE_MESSAGE = "group_mode_archive1_firstline";

    public static final String MESSAGE_IN_INBOX1 = "message_in_inbox1";

    public static final String MESSAGE_IN_INBOX2 = "message_in_inbox2";

    public static final String THREAD_IN_INBOX_MESSAGES_IN_DIFF_FOLDERS_SUBJ = "thread_in_inbox";

    public static final String MESSAGE1_FIRSTLINE1 = "firstline1";

    public static final String MESSAGE1_FIRSTLINE2 = "firstline2";

    public static final String MESSAGE1_FIRSTLINE3 = "firstline3";

    public static final String MOVING_MARKING_FOLDER = "GroupMovingMarking";

    public static final String MARK_WITH_LABEL_SINGLE_MESSAGE = "group_mode_mark_with_label1";

    public static final String MOVE_TO_FOLDER_SINGLE_MESSAGE = "group_mode_move_to_folder1";

    public static final String MARK_AS_IMPORTANT_SINGLE_MESSAGE = "group_mode_make_as_important1";

    public static final String UNMARK_AS_IMPORTANT_SINGLE_MESSAGE = "unmark_with_Important_label1";

    public static final String MARK_UNREAD_SINGLE_MESSAGE = "group_mark_as_unread1";

    public static final String UNMARK_LABEL_SINGLE_MESSAGE = "unmark_with_Users_label1";

    public static final String MARK_AS_READ_SINGLE_MESSAGE = "unmark_with_Unread_label1";

    public static final String GROUP_OPERATIONS_THREADS_FOLDER = "GroupModeThreads";

    public static final String MOVE_TO_ACTION_FULL_THREAD1 = "group_mode_moveto_fullthread1";

    public static final String MOVE_TO_ACTION_FULL_THREAD2 = "group_mode_moveto_fullthread2";

    public static final String MOVE_TO_ACTION_FULL_THREAD3 = "group_mode_moveto_fullthread3";

    public static final String TEST_FAKE_ADS_FOLDER = "Fake ads";

    public static final String UNSPAM_MESSAGE = "unspam_single_message";

    public static final String DELETE_FROM_TRASH_FOLDER_MESSAGE_SUBJ = "delete_from_trash_folder_message";

    public static final String MESSAGE_IN_SPAM_FOLDER = "message_in_spam_folder";

    public static final String CHECK_META_MESSAGE_SUBJECT = "check_meta_message";

    public static final String CHECK_META_MESSAGE_FIRSTLINE = "check_meta_message_firstline";

    public static final String MESSAGE_WITH_ATTACH_CHECK_META = "message_with_attach_in_inbox";

    public MessageListFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        account.addArchiveFolder();
        LabelWrapper label = account.newLabel(LABEL_NAME).build();
        account.addLabels(label);

        FolderWrapper movingMarkingFolder = account.newFolder(MOVING_MARKING_FOLDER).build();
        FolderWrapper folderToMoveIn = account.newFolder(USERS_FOLDER_MEDUZA).build();
        FolderWrapper groupOperationThreadFolder = account.newFolder(GROUP_OPERATIONS_THREADS_FOLDER).build();
        FolderWrapper testFakeAdsFolder = account.newFolder(TEST_FAKE_ADS_FOLDER).build();

        account.addFolders(movingMarkingFolder, folderToMoveIn, groupOperationThreadFolder, testFakeAdsFolder);

        AttachmentWrapper attachment = AttachmentWrapper.newTextAttachment(MESSAGE_WITH_ATTACH_CHECK_META, "");

        final List<String> subjects = listOf(
                MARK_WITH_LABEL_SINGLE_MESSAGE, MOVE_TO_FOLDER_SINGLE_MESSAGE,
                MARK_AS_IMPORTANT_SINGLE_MESSAGE, MARK_UNREAD_SINGLE_MESSAGE,
                MOVE_TO_SPAM_SINGLE_MESSAGE, DELETE_SINGLE_MESSAGE, ARCHIVE_SINGLE_MESSAGE
        );
        account.addMessages(
                map(subjects,
                        subject -> account
                                .newReadMessage(movingMarkingFolder).subjText(subject).build()
                )
        );
        account.addMessages(
                account.newUnreadMessage(movingMarkingFolder)
                        .subjText(MARK_AS_READ_SINGLE_MESSAGE)
                        .build(),
                account.newReadMessage(movingMarkingFolder)
                        .subjText(UNMARK_LABEL_SINGLE_MESSAGE)
                        .labels(label)
                        .build(),
                account.newReadMessage(movingMarkingFolder)
                        .subjText(UNMARK_AS_IMPORTANT_SINGLE_MESSAGE)
                        .labels(account.getImportantLabel())
                        .build(),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_IN_INBOX1)
                        .build(),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(MESSAGE_IN_INBOX2)
                        .build(),
                account.newReadMessage(account.getTrashFolder())
                        .subjText(DELETE_FROM_TRASH_FOLDER_MESSAGE_SUBJ)
                        .build(),
                account.newReadMessage(account.getSpamFolder())
                        .subjText(UNSPAM_MESSAGE)
                        .build(),
                account.newReadMessage(account.getSpamFolder())
                        .subjText(MESSAGE_IN_SPAM_FOLDER)
                        .build(),
                account.newUnreadMessage(account.getInboxFolder())
                        .subjText(CHECK_META_MESSAGE_SUBJECT)
                        .content(CHECK_META_MESSAGE_FIRSTLINE)
                        .timestamp(convertStringToDate(MESSAGE_DATE_TEXT))
                        .to(ADDRESS_RECIPIENT_TO)
                        .attachment(attachment)
                        .labels(label, account.getImportantLabel())
                        .build()
        );
        account.addThreads(
                account.newThread(
                        map(until(0, MESSAGE_COUNT),
                                i -> account
                                        .newReadMessage(groupOperationThreadFolder)
                                        .subjText(MOVE_TO_ACTION_FULL_THREAD1)
                                        .content(String.format(MOVE_TO_ACTION_FULL_THREAD1 + "%d", i))
                        )
                ).build(),
                account.newThread(
                        map(until(0, MESSAGE_COUNT),
                                i -> account
                                        .newReadMessage(groupOperationThreadFolder)
                                        .subjText(MOVE_TO_ACTION_FULL_THREAD2)
                                        .content(String.format(MOVE_TO_ACTION_FULL_THREAD2 + "%d", i))
                        )
                ).build(),
                account.newThread(
                        map(until(0, MESSAGE_COUNT),
                                i -> account
                                        .newReadMessage(groupOperationThreadFolder)
                                        .subjText(MOVE_TO_ACTION_FULL_THREAD3)
                                        .content(String.format(MOVE_TO_ACTION_FULL_THREAD3 + "%d", i))
                        )
                ).build(),
                account.newThread(
                        account
                                .newReadMessage(account.getInboxFolder())
                                .subjText(THREAD_IN_INBOX_MESSAGES_IN_DIFF_FOLDERS_SUBJ)
                                .content(MESSAGE1_FIRSTLINE1),
                        account.newReadMessage(movingMarkingFolder)
                                .subjText(THREAD_IN_INBOX_MESSAGES_IN_DIFF_FOLDERS_SUBJ)
                                .content(MESSAGE1_FIRSTLINE2),
                        account.newReadMessage(account.getInboxFolder())
                                .subjText(THREAD_IN_INBOX_MESSAGES_IN_DIFF_FOLDERS_SUBJ)
                                .content(MESSAGE1_FIRSTLINE3)
                ).build()
        );
    }
}

