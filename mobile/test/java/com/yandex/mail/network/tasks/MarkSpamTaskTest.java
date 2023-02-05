package com.yandex.mail.network.tasks;

import com.yandex.mail.entity.MessageMeta;
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

import androidx.annotation.NonNull;
import kotlin.collections.CollectionsKt;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class MarkSpamTaskTest extends BaseIntegrationTest {

    private List<User.LocalMessage> messages;

    @SuppressWarnings("NullableProblems") // initialized in @Before
    @NonNull
    private MessagesModel messagesModel;

    @Before
    public void setup() {
        init(Accounts.testLoginData);

        List<MessageWrapper> serverMessages = CollectionsKt.listOf(
                account.newReadMessage(serverInbox())
                        .content("Inbox message!")
                        .build(),
                account.newReadMessage(serverTrash())
                        .content("Trash message!")
                        .build()
        );
        account.addMessages(serverMessages);
        user.fetchMessages(inbox(), trash());

        messages = CollectionsKt.map(serverMessages, user::getLocalMessage);

        messagesModel = accountComponent.messagesModel();
    }

    public void markAsSpamHelper(@NonNull List<User.LocalMessage> messagesToMark) throws Exception {
        MarkSpamTask task = MarkSpamTask.create(
                IntegrationTestRunner.app(),
                user.getUid(),
                CollectionsKt.map(messagesToMark, m -> Long.valueOf(m.getServerMid())),
                Long.parseLong(inbox().getServerFid())
        );
        task.updateDatabase(IntegrationTestRunner.app());
    }

    @Test
    public void testMarkSpam() throws Exception {
        markAsSpamHelper(messages);

        final List<Long> messageIds = CollectionsKt.map(messages, message -> Long.valueOf(message.getServerMid()));
        List<MessageMeta> messageMetas = messagesModel.getMessagesMetaByMids(messageIds)
                .blockingGet();

        for (MessageMeta meta : messageMetas) {
            assertThat(meta.getFid()).isEqualTo(Long.parseLong(serverSpam().getServerFid()));
        }
    }
}
