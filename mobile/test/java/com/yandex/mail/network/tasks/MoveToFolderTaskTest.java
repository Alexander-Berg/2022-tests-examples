package com.yandex.mail.network.tasks;

import android.annotation.SuppressLint;

import com.yandex.mail.entity.Message;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;
import com.yandex.mail.wrappers.MessageWrapper;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.asserts.ThreadConditions.fake;
import static com.yandex.mail.provider.Constants.NO_THREAD_ID;
import static com.yandex.mail.util.SolidUtils.collect;
import static com.yandex.mail.util.mailbox.Mailbox.nonThreaded;
import static com.yandex.mail.util.mailbox.Mailbox.threaded;
import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Thread.createThread;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class MoveToFolderTaskTest extends BaseIntegrationTest {

    private static final long CUSTOM_FID = 100000;

    @Before
    public void setUp() throws Exception {
        init(Accounts.testLoginData);
    }

    @Test
    public void shouldMoveFromInboxToTrash() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(createFolder().folderId(inboxFid()).addReadMessages(1))
                .folder(createFolder().folderId(trashFid()))
                .applyAndSync();

        moveHelper(
                mailbox.folder(inboxFid()).messages(),
                inboxFid(),
                trashFid()
        );

        assertThat(mailbox.folder(inboxFid()).threads()).isEmpty();
        assertThat(mailbox.folder(trashFid()).messages()).hasSize(1);
    }

    @Test
    public void shouldMoveFromTrashToInbox() throws Exception {
        Mailbox mailbox = nonThreaded(this)
                .folder(createFolder().folderId(inboxFid()))
                .folder(
                        createFolder()
                                .folderId(trashFid())
                                .addUnreadMessages(1)
                                .addReadMessages(1)
                )
                .applyAndSync();

        moveHelper(
                mailbox.folder(trashFid()).messages(),
                trashFid(),
                inboxFid()
        );

        assertThat(mailbox.folder(trashFid()).messages()).isEmpty();
        assertThat(mailbox.folder(inboxFid()).messages()).hasSize(2);
    }

    @Test
    public void shouldMoveFromSpamToInbox() throws Exception {
        Mailbox mailbox = nonThreaded(this)
                .folder(createFolder().folderId(inboxFid()))
                .folder(
                        createFolder()
                                .folderId(spamFid())
                                .addUnreadMessages(1)
                                .addReadMessages(1)
                )
                .applyAndSync();

        moveHelper(
                mailbox.folder(spamFid()).messages(),
                spamFid(),
                inboxFid()
        );

        assertThat(mailbox.folder(spamFid()).messages()).isEmpty();
        assertThat(mailbox.folder(inboxFid()).messages()).hasSize(2);
    }

    @Test
    public void shouldCreateThreadsInTargetFolderAndDeleteFromCurrent() throws Exception {
        Mailbox mailbox = threaded(this)
                .thread(
                        createThread()
                                .folder(
                                        createFolder()
                                                .folderId(CUSTOM_FID)
                                                .addReadMessages(1)
                                                .addUnreadMessages(1)
                                )
                )
                .applyAndSync();

        List<Message> messages = mailbox.folder(CUSTOM_FID).threads().get(0).messages();

        moveHelper(messages, CUSTOM_FID, inboxFid());

        assertThat(mailbox.folder(CUSTOM_FID).threads()).hasSize(0);
        assertThat(mailbox.folder(inboxFid()).threads()).hasSize(1);
    }

    @Test
    public void shouldCorrectlyMoveSingleMessageFromThread() throws Exception {
        Mailbox mailbox = threaded(this)
                .thread(
                        createThread()
                                .folder(
                                        createFolder()
                                                .folderId(CUSTOM_FID)
                                                .addReadMessages(1)
                                                .addUnreadMessages(1)
                                )
                )
                .applyAndSync();

        List<Message> messages = mailbox.folder(CUSTOM_FID).threads().get(0).messages();

        moveHelper(messages.subList(0, 1), CUSTOM_FID, inboxFid());

        assertThat(mailbox.folder(CUSTOM_FID).threads()).hasSize(1);
        assertThat(mailbox.folder(inboxFid()).threads()).hasSize(1);
    }

    @Test
    public void shouldCreateFakeThreadsIfMovedFromThrash() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(
                        createFolder()
                                .folderId(trashFid())
                                .addUnreadMessages(3)
                                .addReadMessages(3)
                )
                .applyAndSync();

        testFakeThreads(mailbox, trashFid(), inboxFid(), true, true);
    }

    @Test
    public void shouldCreateFakeThreadsIfMovedFromSpam() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(
                        createFolder()
                                .folderId(spamFid())
                                .addUnreadMessages(3)
                                .addReadMessages(3)
                )
                .applyAndSync();

        testFakeThreads(mailbox, spamFid(), inboxFid(), true, true);
    }

    @Test
    public void shouldNotCreateFakeThreadsIfMovedFromUserFolder() throws Exception {
        Mailbox mailbox = threaded(this)
                .thread(
                        createThread()
                                .folder(
                                        createFolder()
                                                .folderId(CUSTOM_FID)
                                                .addUnreadMessages(3)
                                                .addReadMessages(3)
                                )
                )
                .applyAndSync();

        //noinspection ConstantConditions
        testFakeThreads(mailbox, CUSTOM_FID, inboxFid(), false, true);
    }

    @Test
    public void shouldNotCreateFakeThreadsIfMovedToUserFolder() throws Exception {
        Mailbox mailbox = threaded(this)
                .thread(
                        createThread()
                                .folder(
                                        createFolder()
                                                .folderId(inboxFid())
                                                .addUnreadMessages(3)
                                                .addReadMessages(3)
                                )
                )
                .folder(createFolder().folderId(CUSTOM_FID))
                .applyAndSync();

        //noinspection ConstantConditions
        testFakeThreads(mailbox, inboxFid(), CUSTOM_FID, false, false);
    }

    @Test
    public void shouldNotCreateFakeThreadsIfMovedFromThreadedFolderToDrafts() throws Exception {
        Mailbox mailbox = threaded(this)
                .thread(
                        createThread()
                                .folder(
                                        createFolder()
                                                .folderId(inboxFid())
                                                .addUnreadMessages(3)
                                                .addReadMessages(3)
                                )
                )
                .folder(createFolder().folderId(draftsFid()))
                .applyAndSync();

        //noinspection ConstantConditions
        testFakeThreads(mailbox, inboxFid(), draftsFid(), false, false);
    }

    @Test
    public void fakeThreadIdShouldNotBeEqualToNoThreadId() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(
                        createFolder()
                                .folderId(spamFid())
                                .addUnreadMessages(3)
                                .addReadMessages(3)
                )
                .applyAndSync();

        moveHelper(mailbox.folder(spamFid()).messages(), spamFid(), inboxFid());

        List<Mailbox.ClientThread> threads = mailbox.folder(inboxFid()).threads();

        assertThat(threads).doNotHave(new Condition<Mailbox.ClientThread>() {
            @Override
            public boolean matches(Mailbox.ClientThread value) {
                return value.threadId == NO_THREAD_ID;
            }
        });
    }

    @Test
    public void shouldReplaceThreadsWithNonFake() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(
                        createFolder()
                                .folderId(trashFid())
                                .addUnreadMessages(3)
                                .addReadMessages(3)
                )
                .applyAndSync();

        List<Message> messages = mailbox.folder(trashFid()).messages();

        moveHelper(messages, trashFid(), inboxFid());
        // messages in trash are now fake

        // move messages on server
        List<String> messageIds = collect(messages, m -> String.valueOf(m.getMeta().getMid()));
        List<MessageWrapper> serverMessages = CollectionsKt.filter(account.messages.messages,
                w -> messageIds.contains(w.getMid())
        );
        account.moveMessages(serverMessages, serverInbox());

        messages = mailbox.folder(inboxFid()).messages();
        user.fetchMessagesNano(inboxFid());
        assertThat(CollectionsKt.map(messages, message -> message.getMeta().getMid())).areNot(fake());
    }

    @Test
    public void shouldRemoveFakeThreads() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(
                        createFolder()
                                .folderId(trashFid())
                                .addUnreadMessages(3)
                                .addReadMessages(3)
                )
                .applyAndSync();

        List<Message> messages = mailbox.folder(trashFid()).messages();

        moveHelper(messages, trashFid(), inboxFid());
        // messages in trash are now fake

        // move messages on server
        List<String> messageIds = collect(messages, m -> String.valueOf(m.getMeta().getMid()));
        List<MessageWrapper> serverMessages = CollectionsKt.filter(account.messages.messages,
                w -> messageIds.contains(w.getMid())
        );
        account.moveMessages(serverMessages, serverInbox());

        user.fetchMessagesNano(inboxFid());
        testNoFakeThreads();
    }

    @Test
    public void updateDatabase_shouldSaveReadUnreadStateToFakeThread() throws Exception {
        Mailbox mailbox = threaded(this)
                .folder(
                        createFolder()
                                .folderId(trashFid())
                                .addUnreadMessages(3)
                                .addReadMessages(3)
                )
                .applyAndSync();

        Map<Long, Boolean> mapToReadUnread =
                CollectionsKt.associateBy(mailbox.folder(trashFid()).messages(), m -> m.getMeta().getMid(), m -> m.getMeta().getUnread());

        moveHelper(mailbox.folder(trashFid()).messages(), trashFid(), inboxFid());

        CollectionsKt.forEach(mailbox.folder(inboxFid()).messages(), message -> {
            assertThat(mapToReadUnread.get(message.getMeta().getMid())).isEqualTo(message.getMeta().getUnread());
            return null;
        });
    }

    private void testFakeThreads(
            @NonNull Mailbox mailbox,
            long sourceFolderId,
            long targetFolderId,
            boolean shouldCreateFake,
            boolean shouldBeUnique
    ) throws Exception {
        List<Message> messages;
        if (shouldCreateFake) {
            // if move should create fake threads we have flat folder without threads
            messages = mailbox.folder(sourceFolderId).messages();
        } else {
            messages = mailbox.folder(sourceFolderId).messages();
        }

        moveHelper(messages, sourceFolderId, targetFolderId);

        messages = mailbox.folder(sourceFolderId).messages();

        for (Message message : messages) {
            if (shouldCreateFake) {
                assertThat(message.getMeta().getTid()).is(fake());
            } else {
                assertThat(message.getMeta().getTid()).isNot(fake());
            }
        }

        if (shouldBeUnique) {
            final List<Long> messageTids = CollectionsKt.map(messages, message -> message.getMeta().getTid());
            assertThat(CollectionsKt.distinct(messageTids)).hasSize(messages.size());
        }
    }

    private void moveHelper(
            @NonNull List<Message> messages,
            long fromFid,
            long toFid
    ) throws Exception {
        MoveToFolderTask task = new MoveToFolderTask(
                IntegrationTestRunner.app(),
                collect(messages, message -> message.getMeta().getMid()),
                toFid,
                fromFid,
                user.getUid()
        );
        task.updateDatabase(IntegrationTestRunner.app());
        task.performNetworkOperationRetrofit(app);
    }

    private void testNoFakeThreads() { // all fake threads have tid < 0
        assertThat(threadsModel.getMinimalTid().blockingGet() >= 0).isTrue();
    }
}
