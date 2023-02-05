package com.yandex.mail.network.tasks;

import android.annotation.SuppressLint;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.util.mailbox.Mailbox.nonThreaded;
import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Message.createMessage;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class MarkWithLabelTaskTest extends BaseIntegrationTest {

    @Before
    public void setUp() throws Exception {
        init(Accounts.testLoginData);
    }

    @Test
    public void testMark() throws Exception {
        Mailbox mailbox = nonThreaded(this)
                .folder(
                        createFolder()
                            .folderId(inboxFid())
                            .addReadMessages(1)
                            .addUnreadMessages(2)
                )
                .applyAndSync();

        markHelper(CollectionsKt.map(mailbox.folder(inboxFid()).messages().subList(0, 2), m -> m.getMeta().getMid()));

        int important = CollectionsKt.sumBy(
                mailbox.folder(inboxFid()).messages(),
                message -> message.getLabels().contains(serverImportant().getServerLid()) ? 1 : 0
        );

        assertThat(important).isEqualTo(2);
    }

    @Test
    public void testUnmark() throws Exception {
        Mailbox mailbox = Mailbox.nonThreaded(this)
                .folder(
                        createFolder()
                            .folderId(inboxFid())
                            .addMessage(createMessage().label(serverImportant().getServerLid()))
                            .addMessage(createMessage().label(serverImportant().getServerLid()))
                            .addMessage(createMessage().label(serverImportant().getServerLid()))
                )
                .applyAndSync();

        unmarkHelper(CollectionsKt.map(mailbox.folder(inboxFid()).messages().subList(0, 2), m -> m.getMeta().getMid()));

        int important = CollectionsKt.sumBy(
                mailbox.folder(inboxFid()).messages(),
                message -> message.getLabels().contains(serverImportant().getServerLid()) ? 1 : 0
        );

        assertThat(important).isEqualTo(1);
    }

    public void helper(boolean mark, @NonNull List<Long> messageIds) throws Exception {
        MarkWithLabelTask task = new MarkWithLabelTask(
                IntegrationTestRunner.app(),
                messageIds,
                mark,
                important().getServerLid(),
                user.getUid()
        );
        task.updateDatabase(IntegrationTestRunner.app());
    }

    public void markHelper(@NonNull List<Long> messageIds) throws Exception {
        helper(true, messageIds);
    }

    public void unmarkHelper(@NonNull List<Long> messageIds) throws Exception {
        helper(false, messageIds);
    }
}
