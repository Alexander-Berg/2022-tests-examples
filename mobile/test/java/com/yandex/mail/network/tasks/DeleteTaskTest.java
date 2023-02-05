package com.yandex.mail.network.tasks;

import android.annotation.SuppressLint;

import com.yandex.mail.entity.MessageMeta;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.MessagesModel;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.wrappers.MessageWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.util.SolidUtils.collect;
import static java.lang.Long.parseLong;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;

//TODO: test counters
@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class DeleteTaskTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private MessagesModel messagesModel;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private FoldersModel foldersModel;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private List<User.LocalMessage> inboxMessages;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private List<User.LocalMessage> draftMessages;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private List<User.LocalMessage> trashMessages;

    @Before
    public void setup() throws Exception {
        init(Accounts.testLoginData);

        List<MessageWrapper> sInboxMessages = listOf(
                account.newUnreadMessage(serverInbox())
                        .content("Inbox message 1 (unread)!")
                        .build(),
                account.newReadMessage(serverInbox())
                        .content("Inbox message 2!")
                        .build()
        );
        List<MessageWrapper> sDraftMessages = listOf(
                account.newReadMessage(serverDrafts())
                        .content("Draft message 1!")
                        .build()
        );
        List<MessageWrapper> sTrashMessages = listOf(
                account.newReadMessage(serverTrash())
                        .content("Trash message 1!")
                        .build()
        );
        account.addMessages(CollectionsKt.flatten(listOf(sInboxMessages, sDraftMessages, sTrashMessages)));

        user.fetchMessages(inbox(), trash(), drafts());

        inboxMessages = CollectionsKt.map(sInboxMessages, user::getLocalMessage);
        draftMessages = CollectionsKt.map(sDraftMessages, user::getLocalMessage);
        trashMessages = CollectionsKt.map(sTrashMessages, user::getLocalMessage);

        messagesModel = accountComponent.messagesModel();
        foldersModel = accountComponent.foldersModel();
    }

    public void testHelper(@NonNull List<User.LocalMessage> messages) throws Exception {
       testHelper(messages, false);
    }

    public void testHelper(@NonNull List<User.LocalMessage> messages, boolean fromPush) throws Exception {
        DeleteTask task = DeleteTask.create(
                IntegrationTestRunner.app(),
                collect(messages, m -> parseLong(m.getServerMid())),
                user.getUid(),
                fromPush
        );
        task.updateDatabase(IntegrationTestRunner.app());
    }

    @Test
    public void testDeleteSome() throws Exception {
        User.LocalMessage toDelete = inboxMessages.get(0);
        testHelper(listOf(toDelete));

        assertInFolder(Long.parseLong(serverTrash().getServerFid()), listOf(toDelete));
    }

    @Test
    public void testDeleteFromNonTrash() throws Exception {
        List<User.LocalMessage> messages = CollectionsKt.flatten(listOf(inboxMessages, draftMessages));
        testHelper(messages);

        assertInFolder(Long.parseLong(serverTrash().getServerFid()), messages);
    }

    @Test
    public void testDeleteFromTrash() throws Exception {
        testHelper(trashMessages);
        assertExists(inboxMessages);
        assertExists(draftMessages);
        assertNotExists(trashMessages);
    }

    @Test
    public void testDeleteFromTrashIfPush() throws Exception {
        testHelper(trashMessages, true);
        assertInFolder(Long.parseLong(serverTrash().getServerFid()), trashMessages);
    }

    @Test
    public void testDeleteMixed() throws Exception {
        List<User.LocalMessage> messages = CollectionsKt.flatten(listOf(inboxMessages, trashMessages, draftMessages));
        testHelper(messages);

        assertInFolder(Long.parseLong(serverTrash().getServerFid()), inboxMessages);
        assertNotExists(trashMessages);
        assertInFolder(Long.parseLong(serverTrash().getServerFid()), draftMessages);
    }

    private void assertInFolder(long folderId, @NonNull List<User.LocalMessage> messages) {
        List<MessageMeta> messageMetas = messagesModel
                .getMessagesMetaByMids(collect(messages, m -> Long.valueOf(m.getServerMid())))
                .blockingGet();

        CollectionsKt.forEach(messageMetas, value -> {
            assertThat(value.getFid()).isEqualTo(folderId);
            return null;
        });
    }

    private void assertExists(@NonNull List<User.LocalMessage> localMessages) {
        existsHelper(localMessages, true);
    }

    private void assertNotExists(@NonNull List<User.LocalMessage> localMessages) {
        existsHelper(localMessages, false);
    }

    private void existsHelper(@NonNull List<User.LocalMessage> localMessages, boolean exists) {
        List<Long> allMessages = CollectionsKt.map(messagesModel.getAllMessagesMeta().blockingGet(), MessageMeta::getMid);
        final Set<Long> allMessagesSet = CollectionsKt.toSet(allMessages);

        CollectionsKt.forEach(localMessages, m -> {
            assertThat(allMessagesSet.contains(Long.parseLong(m.getServerMid()))).isEqualTo(exists);
            return null;
        });
    }
}
