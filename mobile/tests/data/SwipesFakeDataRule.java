package com.yandex.mail.tests.data;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.rules.FakeAccountRule;
import com.yandex.mail.wrappers.AttachmentWrapper;
import com.yandex.mail.wrappers.FolderWrapper;
import com.yandex.mail.wrappers.LabelWrapper;

import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.pages.MessageViewPage.NAME_ATTACH_FROM_FORWARDING_MESSAGE;
import static com.yandex.mail.util.FoldersAndLabelsConst.LABEL_NAME;
import static com.yandex.mail.util.FoldersAndLabelsConst.USERS_FOLDER_MEDUZA;
import static kotlin.collections.CollectionsKt.map;
import static kotlin.ranges.RangesKt.until;

public class SwipesFakeDataRule extends FakeAccountRule {

    public static final String USER_FOLDER_FOR_SWIPE = "swipe_folder";

    public static final String MESSAGE_FOR_SWIPE1 = "message_for_swipe_action1";

    public static final String MESSAGE_FOR_SWIPE2 = "message_for_swipe_action2";

    public static final String MESSAGE_FOR_SWIPE3 = "message_for_swipe_action3";

    public static final String MESSAGE_FOR_FORWARD_WITH_ATTACH = "message with attach for forward";

    public static final String UNREAD_MESSAGE_FOR_SWIPE = "unread_message_to_swipe";

    public static final String SWIPE_ACTION_FOR_THREAD = "swipe_action_for_thread";

    public static int MESSAGE_COUNT = 3;

    public SwipesFakeDataRule(@NonNull String login) {
        super(login);
    }

    @Override
    public void initialize(@NonNull AccountWrapper account) {
        AttachmentWrapper attachment = AttachmentWrapper.newTextAttachment(NAME_ATTACH_FROM_FORWARDING_MESSAGE, "");

        account.addArchiveFolder();

        FolderWrapper userFolder = account.newFolder(USER_FOLDER_FOR_SWIPE).build();

        FolderWrapper folderToMoveIn = account.newFolder(USERS_FOLDER_MEDUZA).build();

        account.addFolders(userFolder, folderToMoveIn);

        LabelWrapper userLabel = account.newLabel(LABEL_NAME).build();
        account.addLabels(userLabel);

        final List<String> subjects = CollectionsKt.listOf(MESSAGE_FOR_SWIPE1, MESSAGE_FOR_SWIPE2, MESSAGE_FOR_SWIPE3);
        account.addMessages(
                map(subjects,
                                subject -> account
                                        .newReadMessage(userFolder)
                                        .subjText(subject)
                                        .build()
                        )
        );

        account.addMessages(
                account.newUnreadMessage(userFolder)
                        .subjText(UNREAD_MESSAGE_FOR_SWIPE)
                        .build(),
                account.newUnreadMessage(userFolder)
                        .subjText(MESSAGE_FOR_FORWARD_WITH_ATTACH)
                        .attachment(attachment)
                        .build()
        );

        account.addThreads(
                account.newThread(
                        map(until(0, MESSAGE_COUNT),
                                i -> account
                                        .newReadMessage(userFolder)
                                        .subjText(SWIPE_ACTION_FOR_THREAD)
                                        .content(String.format(SWIPE_ACTION_FOR_THREAD + "%d", i))
                        )
                ).build()
        );
    }
}
