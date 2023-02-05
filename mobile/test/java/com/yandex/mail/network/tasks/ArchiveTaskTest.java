package com.yandex.mail.network.tasks;

import android.os.RemoteException;

import com.yandex.mail.entity.Message;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.AccountNotInDBException;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Label.createLabel;
import static com.yandex.mail.util.mailbox.MailboxEditor.Message.createMessage;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class ArchiveTaskTest extends BaseIntegrationTest {

    private static final String CUSTOM_LABEL_1 = "Custom1";

    private long messageId;

    @Before
    public void setup() throws Exception {
        init(Accounts.testLoginData);
        Mailbox mailbox = Mailbox.nonThreaded(this)
                .label(createLabel().labelId(CUSTOM_LABEL_1))
                .folder(createFolder().folderId(inboxFid()).addMessage(createMessage().label(CUSTOM_LABEL_1)))
                .applyAndSync();
        final Mailbox.ClientFolder folder = mailbox.folder(inboxFid());
        messageId = folder.messages().get(0).getMeta().getMid();
    }

    private void archiveMessageHelper() throws AccountNotInDBException, RemoteException {
        ArchiveTask task = ArchiveTask.create(
                IntegrationTestRunner.app(),
                listOf(messageId),
                user.getUid()
        );
        task.updateDatabase(IntegrationTestRunner.app());
    }

    @Test
    public void testCreatesFakeArchive() throws Exception {
        archiveMessageHelper();

        long newFid = messagesModel.getFidByMid(messageId).blockingGet();
        assertThat(newFid).isEqualTo(ArchiveTask.FAKE_ARCHIVE_ID);
    }

    @Test
    public void testArchivePreservesLabels() throws Exception {
        archiveMessageHelper();

        Message message = messagesModel.observeCompositeMessageByMid(messageId).blockingFirst().get();
        assertThat(message.getLabels()).containsExactly(CUSTOM_LABEL_1);
    }
}
