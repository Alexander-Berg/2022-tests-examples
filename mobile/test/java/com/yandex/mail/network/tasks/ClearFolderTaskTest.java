package com.yandex.mail.network.tasks;

import android.annotation.SuppressLint;

import com.yandex.mail.entity.FidWithCounters;
import com.yandex.mail.runners.IntegrationTestRunner;
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
import static org.assertj.core.api.Assertions.assertThat;

@SuppressLint("NewApi")
@RunWith(IntegrationTestRunner.class)
public class ClearFolderTaskTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private Mailbox mailbox;

    @Before
    public void setUp() throws Exception {
        mailbox = nonThreaded(this)
                .folder(
                        createFolder()
                                .folderId(trashFid())
                                .addReadMessages(2)
                                .addUnreadMessages(2)
                )
                .applyAndSync();
    }

    @Test
    public void testClears() throws Exception {
        List<Long> messageIds = CollectionsKt.map(mailbox.folder(trashFid()).messages(), message -> message.getMeta().getMid());
        clearTrashHelper();
        assertThat(CollectionsKt.all(messageIds, id -> !mailbox.isMessageExists(id))).isTrue();
    }

    @Test
    public void clear_trash_drops_counters() throws Exception {
        clearTrashHelper();

        FidWithCounters counters = foldersModel.observeCounters()
                .map(longFidWithCountersMap -> longFidWithCountersMap.get(trashFid()))
                .blockingFirst();

        assertThat(counters.getTotal_counter()).isEqualTo(0);
        assertThat(counters.getUnread_counter()).isEqualTo(0);
    }

    private void clearTrashHelper() throws Exception {
        ClearFolderTask task = new ClearFolderTask(IntegrationTestRunner.app(), trashFid(), user.getUid());
        task.updateDatabase(IntegrationTestRunner.app());
    }
}
