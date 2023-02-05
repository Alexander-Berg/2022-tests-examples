package com.yandex.mail.network.tasks;

import android.annotation.SuppressLint;

import com.yandex.mail.entity.Message;
import com.yandex.mail.entity.MessageMeta;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.util.SolidUtils.collect;
import static com.yandex.mail.util.mailbox.Mailbox.nonThreaded;
import static com.yandex.mail.util.mailbox.Mailbox.threaded;
import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Thread.createThread;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class ClearMessageTaskTest extends BaseIntegrationTest {

    @Before
    public void setUp() throws Exception {
        init(Accounts.testLoginData);
    }

    @Test
    public void testClearFromNonOutgoing() throws Exception {
        Mailbox mailbox = nonThreaded(this)
                .folder(
                        createFolder()
                                .folderId(inboxFid())
                                .addReadMessages(1)
                                .addUnreadMessages(1)
                )
                .applyAndSync();

        long toDeleteId = mailbox.folder(inboxFid()).messages().get(0).getMeta().getMid();
        long toRetainId = mailbox.folder(inboxFid()).messages().get(1).getMeta().getMid();

        testClearHelper(CollectionsKt.map(mailbox.folder(inboxFid()).messages().subList(0, 1), Message::getMeta));

        assertThat(mailbox.isMessageExists(toDeleteId)).isFalse();
        assertThat(mailbox.isMessageExists(toRetainId)).isTrue();
    }

    @Test
    public void testClearFromOutgoing() throws Exception {
        Mailbox mailbox = nonThreaded(this)
                .folder(
                        createFolder()
                                .folderId(outgoingFid())
                                .addReadMessages(1)
                                .addUnreadMessages(1)
                )
                .applyAndSync();

        long toRetainId = mailbox.folder(outgoingFid()).messages().get(1).getMeta().getMid();

        testClearHelper(CollectionsKt.map(mailbox.folder(outgoingFid()).messages().subList(0, 1), Message::getMeta));

        assertThat(mailbox.isMessageExists(toRetainId)).isTrue();
    }

    @Test
    public void testClearFromNonOutgoingAndFromOutgoing() throws Exception {
        Mailbox mailbox = nonThreaded(this)
                .folder(
                        createFolder()
                                .folderId(inboxFid())
                                .addReadMessages(1)
                                .addUnreadMessages(1)
                )
                .folder(
                        createFolder()
                                .folderId(outgoingFid())
                                .addReadMessages(1)
                                .addUnreadMessages(1)
                )
                .applyAndSync();

        List<Long> toDeleteIds = CollectionsKt.map(mailbox.folder(inboxFid()).messages(), message -> message.getMeta().getMid());
        List<Long> toRetainIds = CollectionsKt.map(mailbox.folder(outgoingFid()).messages(), message -> message.getMeta().getMid());

        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(mailbox.folder(inboxFid()).messages());
        allMessages.addAll(mailbox.folder(outgoingFid()).messages());

        testClearHelper(CollectionsKt.map(allMessages, Message::getMeta));

        assertThat(CollectionsKt.all(toDeleteIds, id -> !mailbox.isMessageExists(id))).isTrue();
        assertThat(CollectionsKt.all(toRetainIds, mailbox::isMessageExists)).isTrue();
    }

    @Test
    public void testClearFromNonOutgoingAndFromOutgoingThreaded() throws Exception {
        Mailbox mailbox = threaded(this)
                .thread(createThread().folder(createFolder().folderId(inboxFid()).addReadMessages(1)))
                .folder(
                        createFolder().folderId(outgoingFid())
                                .addReadMessages(1)
                                .addUnreadMessages(1)
                )
                .applyAndSync();

        List<Long> toDeleteIds = CollectionsKt.listOf(mailbox.folder(inboxFid()).threads().get(0).thread().getTopMessageMeta().getMid());
        List<Long> toRetainIds = CollectionsKt.map(mailbox.folder(outgoingFid()).messages(), message -> message.getMeta().getMid());

        List<MessageMeta> allMessages = new ArrayList<>();
        allMessages.add(mailbox.folder(inboxFid()).threads().get(0).thread().getTopMessageMeta());
        allMessages.addAll(CollectionsKt.map(mailbox.folder(outgoingFid()).messages(), Message::getMeta));

        testClearHelper(allMessages);

        assertThat(CollectionsKt.all(toDeleteIds, id -> !mailbox.isMessageExists(id))).isTrue();
        assertThat(CollectionsKt.all(toRetainIds, mailbox::isMessageExists)).isTrue();
    }

    private void testClearHelper(@NonNull Iterable<MessageMeta> messages) throws Exception {
        ClearMessagesTask task = new ClearMessagesTask(
                IntegrationTestRunner.app(),
                collect(messages, MessageMeta::getMid),
                user.getUid()
        );
        task.updateDatabase(IntegrationTestRunner.app());
    }
}
