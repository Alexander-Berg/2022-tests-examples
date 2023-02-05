

package com.yandex.mail.network.tasks;

import android.annotation.SuppressLint;

import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.util.BaseIntegrationTest;
import com.yandex.mail.util.mailbox.Mailbox;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder;
import static com.yandex.mail.util.mailbox.MailboxEditor.Label.createLabel;
import static com.yandex.mail.util.mailbox.MailboxEditor.Message.createMessage;
import static kotlin.collections.CollectionsKt.listOf;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class MultiMarkWithLabelTaskOfflineTest extends BaseIntegrationTest {

    private static final String CUSTOM_LABEL_ID = "custom";

    @Before
    public void setUp() throws Exception {
        init(Accounts.testLoginData);
    }

    @Test
    public void testMultiMark() throws Exception {
        Mailbox mailbox = Mailbox.nonThreaded(this)
                .label(createLabel().labelId(CUSTOM_LABEL_ID))
                .folder(
                        createFolder()
                            .folderId(inboxFid())
                            .addReadMessages(1)
                            .addUnreadMessages(2)
                            .addMessage(createMessage().label(CUSTOM_LABEL_ID))
                )
                .applyAndSync();

        markHelper(
                CollectionsKt.map(mailbox.folder(inboxFid()).messages().subList(0, 2), m -> m.getMeta().getMid()),
                listOf(serverImportant().getServerLid(), CUSTOM_LABEL_ID)
        );

        int labelsCount = CollectionsKt.sumBy(mailbox.folder(inboxFid()).messages(), message -> message.getLabels().size());

        assertThat(labelsCount).isEqualTo(5);
    }

    @Test
    public void testMultiUnmark() throws Exception {
        Mailbox mailbox = Mailbox.nonThreaded(this)
                .label(createLabel().labelId(CUSTOM_LABEL_ID))
                .folder(
                        createFolder()
                            .folderId(inboxFid())
                            .addMessage(createMessage().label(serverImportant().getServerLid()))
                            .addMessage(createMessage().label(CUSTOM_LABEL_ID))
                            .addMessage(createMessage())
                            .addMessage(createMessage().label(CUSTOM_LABEL_ID))
                )
                .applyAndSync();

        unmarkHelper(
                CollectionsKt.map(mailbox.folder(inboxFid()).messages().subList(0, 3), m -> m.getMeta().getMid()),
                listOf(serverImportant().getServerLid(), CUSTOM_LABEL_ID)
        );

        int labelsCount = CollectionsKt.sumBy(mailbox.folder(inboxFid()).messages(), message -> message.getLabels().size());

        assertThat(labelsCount).isEqualTo(1);
    }

    @Test
    public void testMixed() throws Exception {
        Mailbox mailbox = Mailbox.nonThreaded(this)
                .label(createLabel().labelId(CUSTOM_LABEL_ID))
                .folder(
                        createFolder()
                            .folderId(inboxFid())
                            .addMessage(createMessage().label(serverImportant().getServerLid()))
                            .addMessage(createMessage().label(CUSTOM_LABEL_ID))
                            .addMessage(createMessage())
                )
                .applyAndSync();

        Map<String, Boolean> markMap = new HashMap<>();
        markMap.put(serverImportant().getServerLid(), true);
        markMap.put(CUSTOM_LABEL_ID, false);

        MultiMarkWithLabelTaskOffline task = new MultiMarkWithLabelTaskOffline(
                IntegrationTestRunner.app(),
                user.getUid(),
                markMap,
                CollectionsKt.map(mailbox.folder(inboxFid()).messages(), m -> m.getMeta().getMid()),
                listOf(serverImportant().getServerLid(), CUSTOM_LABEL_ID)
        );
        task.updateDatabase(IntegrationTestRunner.app());

        int importantCount = CollectionsKt.sumBy(
                mailbox.folder(inboxFid()).messages(),
                message -> message.getLabels().contains(serverImportant().getServerLid()) ? 1 : 0
        );

        int customCount = CollectionsKt.sumBy(mailbox.folder(inboxFid()).messages(), message -> message.getLabels().contains(CUSTOM_LABEL_ID) ? 1 : 0);

        assertThat(importantCount).isEqualTo(3);
        assertThat(customCount).isEqualTo(0);
    }

    private void helper(boolean mark, @NonNull List<Long> messageIds, @NonNull List<String> labelIds) throws Exception {
        MultiMarkWithLabelTaskOffline task = new MultiMarkWithLabelTaskOffline(
                IntegrationTestRunner.app(),
                user.getUid(),
                fillMapWithConstant(labelIds, mark),
                messageIds,
                labelIds
        );
        task.updateDatabase(IntegrationTestRunner.app());
    }

    private void markHelper(@NonNull List<Long> messageIds, @NonNull List<String> labelIds) throws Exception {
        helper(true, messageIds, labelIds);
    }

    private void unmarkHelper(@NonNull List<Long> messageIds, @NonNull List<String> labelIds) throws Exception {
        helper(false, messageIds, labelIds);
    }

    @NonNull
    private static <K, V> Map<K, V> fillMapWithConstant(@NonNull Iterable<K> keys, @Nullable V value) {
        Map<K, V> map = new HashMap<>();
        for (K key : keys) {
            map.put(key, value);
        }
        return map;
    }
}
