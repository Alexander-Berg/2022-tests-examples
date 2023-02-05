package com.yandex.mail.react.model;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.ApplicationComponent;
import com.yandex.mail.entity.FolderType;
import com.yandex.mail.filters.promo.FilterPromoChainModel;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.LabelsModel;
import com.yandex.mail.model.MessagesModel;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mail.service.CommandsServiceActions;
import com.yandex.mail.tools.TestWorkerFactory.WorkerInfo;
import com.yandex.mail.util.BaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import kotlin.collections.CollectionsKt;

import static com.yandex.mail.provider.Constants.CURRENT_FOLDER_ID_EXTRA;
import static com.yandex.mail.provider.Constants.LABEL_ID_EXTRAS;
import static com.yandex.mail.provider.Constants.MARK_EXTRAS;
import static com.yandex.mail.provider.Constants.MESSAGE_ID_EXTRAS;
import static com.yandex.mail.provider.Constants.NO_FOLDER_ID;
import static com.yandex.mail.provider.Constants.SHOULD_SEND_TO_SERVER;
import static com.yandex.mail.provider.Constants.UID_EXTRA;
import static com.yandex.mail.service.CommandsServiceActions.MULTI_MARK_WITH_LABEL_ACTION_OFFLINE;
import static com.yandex.mail.util.NanomailEntitiesTestUtils.createTestLabel;
import static io.reactivex.Single.just;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static kotlin.collections.ArraysKt.toList;
import static kotlin.collections.CollectionsKt.listOf;
import static kotlin.collections.CollectionsKt.toLongArray;
import static kotlin.collections.SetsKt.emptySet;
import static kotlin.collections.SetsKt.setOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(UnitTestRunner.class)
public class MailModelTest extends BaseIntegrationTest {

    private static final String IMPORTANT_LABEL_ID = "important";

    @SuppressWarnings("NullableProblems") // @Before
    @Mock
    @NonNull
    BaseMailApplication mailApplication;

    @SuppressWarnings("NullableProblems") // @Before
    @Mock
    @NonNull
    FilterPromoChainModel filterPromoChainModel;

    private MailModel mailModel;

    @Before
    public void beforeEachTest() {
        initBase();
        initMocks(this);
        when(mailApplication.getApplicationContext()).thenReturn(mailApplication);
        final ApplicationComponent appComponent = mock(ApplicationComponent.class);
        when(mailApplication.getApplicationComponent()).thenReturn(appComponent);
        mailModel = new MailModel(mailApplication, filterPromoChainModel);
    }

    @Test
    public void markAsReadShouldCompleteIfListOfMessagesIsEmpty() {
        long uid = 1;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver = mailModel.markAsRead(uid, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();
        testObserver.assertNoValues();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void markAsReadShouldSendIntentToCommandService() {
        long uid = 1;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        TestObserver<Void> testObserver = mailModel.markAsRead(uid, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MARK_AS_READ_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void markAsUnreadShouldCompleteIfListOfMessagesIsEmpty() {
        long uid = 1;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver = mailModel.markAsUnread(uid, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();
        testObserver.assertNoValues();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void markAsUnreadShouldSendIntentToTheCommandService() {
        long uid = 1;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        TestObserver<Void> testObserver = mailModel.markAsUnread(uid, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MARK_AS_UNREAD_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void archiveShouldCompleteIfListOfMessageIdsIsEmpty() {
        long uid = 1;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver = mailModel.archive(uid, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();
        testObserver.assertNoValues();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void archiveShouldSendIntentToCommandService() {
        long uid = 1;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        TestObserver<Void> testObserver = mailModel.archive(uid, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.ARCHIVE_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void deleteShouldCompleteIfCollectionOfMessageIdsIsEmpty() {
        long uid = 1;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver = mailModel.delete(uid, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoValues();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void deleteShouldSendIntentToCommandService() {
        long uid = 1;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        AccountComponent accountComponent = mock(AccountComponent.class);
        MessagesModel messagesModel = mock(MessagesModel.class);
        FoldersModel foldersModel = mock(FoldersModel.class);

        when(mailApplication.getAccountComponent(uid))
                .thenReturn(accountComponent);
        when(accountComponent.foldersModel())
                .thenReturn(foldersModel);
        when(foldersModel.getFidByType(FolderType.OUTGOING))
                .thenReturn(Single.just(Optional.of(10L)));
        when(foldersModel.getFidByType(FolderType.DRAFT))
                .thenReturn(Single.just(Optional.of(11L)));

        when(accountComponent.messagesModel())
                .thenReturn(messagesModel);
        when(messagesModel.filterMessagesWithFid(10L, messageIds))
                .thenReturn(Single.just(Collections.emptyList()));
        when(messagesModel.filterMessagesWithFid(11L, messageIds))
                .thenReturn(Single.just(Collections.emptyList()));

        TestObserver<Void> testObserver = mailModel.delete(uid, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.DELETE_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void deleteShouldSendPurgeIntentToCommandServiceForOutgoings() {
        long uid = 1;
        List<Long> messageIds = asList(1L, 2L, 3L);

        AccountComponent accountComponent = mock(AccountComponent.class);
        MessagesModel messagesModel = mock(MessagesModel.class);
        FoldersModel foldersModel = mock(FoldersModel.class);

        when(mailApplication.getAccountComponent(uid))
                .thenReturn(accountComponent);
        when(accountComponent.foldersModel())
                .thenReturn(foldersModel);
        when(foldersModel.getFidByType(FolderType.OUTGOING))
                .thenReturn(Single.just(Optional.of(10L)));
        when(foldersModel.getFidByType(FolderType.DRAFT))
                .thenReturn(Single.just(Optional.of(11L)));

        when(accountComponent.messagesModel())
                .thenReturn(messagesModel);
        when(messagesModel.filterMessagesWithFid(10L, messageIds))
                .thenReturn(Single.just(messageIds));
        when(messagesModel.filterMessagesWithFid(11L, messageIds))
                .thenReturn(Single.just(emptyList()));

        TestObserver<Void> testObserver = mailModel.delete(uid, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.PURGE_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void markAsSpamShouldCompleteIfCollectionOfMessageIdsIsEmpty() {
        long uid = 1;
        long currentLocalFolderId = 2;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver =
                mailModel.markAsSpam(uid, currentLocalFolderId, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoValues();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void markAsSpamShouldSendIntentToCommandService() throws ExecutionException, InterruptedException {
        long uid = 1;
        long currentLocalFolderId = 2;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        TestObserver<Void> testObserver =
                mailModel.markAsSpam(uid, currentLocalFolderId, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MARK_AS_SPAM_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(worker.getInputLong(CURRENT_FOLDER_ID_EXTRA, NO_FOLDER_ID)).isEqualTo(currentLocalFolderId);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void markAsNotSpamShouldCompleteIfCollectionOfMessageIdsIsEmpty() {
        long uid = 1;
        long currentLocalFolderId = 2;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver =
                mailModel.markAsNotSpam(uid, currentLocalFolderId, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoValues();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void markAsNotSpamShouldSendIntentToCommandService() {
        long uid = 1;
        long currentLocalFolderId = 2;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        TestObserver<Void> testObserver =
                mailModel.markAsNotSpam(uid, currentLocalFolderId, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MARK_NOT_SPAM_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(worker.getInputLong(CURRENT_FOLDER_ID_EXTRA, NO_FOLDER_ID)).isEqualTo(currentLocalFolderId);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void markWithLabelShouldCompleteIfCollectionOfMessageIdsIsEmpty() {
        long uid = 1;
        final String labelId = "2";
        boolean mark = true;
        Collection<Long> localMessageIds = emptyList();

        TestObserver<Void> testObserver =
                mailModel.markWithLabel(uid, labelId, mark, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoValues();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void markWithLabelShouldSendIntentToCommandService() {
        long uid = 1;
        final String labelId = "2";
        boolean mark = true;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        TestObserver<Void> testObserver =
                mailModel.markWithLabel(uid, labelId, mark, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MARK_MESSAGE_WITH_LABEL_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(worker.getInputString(LABEL_ID_EXTRAS)).isEqualTo(labelId);
        assertThat(worker.getInputBoolean(MARK_EXTRAS, false)).isEqualTo(mark);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void markAsImportantShouldCompleteIfCollectionOfMessageIdsIsEmpty() {
        long uid = 1;
        boolean mark = true;
        Collection<Long> messageIds = emptyList();

        mockImportant(uid);

        TestObserver<Void> testObserver =
                mailModel.markAsImportant(uid, mark, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoValues();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).isEmpty();
    }

    @Test
    public void markAsImportantShouldSendIntentToCommandService() {
        long uid = 1;
        boolean mark = true;
        Collection<Long> messageIds = asList(1L, 2L, 3L);

        mockImportant(uid);

        TestObserver<Void> testObserver = mailModel.markAsImportant(uid, mark, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(1);

        final WorkerInfo worker = startedWorkers.get(0);
        assertThat(worker.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MARK_MESSAGE_WITH_LABEL_ACTION);
        assertThat(worker.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(worker.getInputString(LABEL_ID_EXTRAS)).isEqualTo(IMPORTANT_LABEL_ID);
        assertThat(worker.getInputBoolean(MARK_EXTRAS, false)).isEqualTo(mark);
        final long[] mids = worker.getInputLongArray(MESSAGE_ID_EXTRAS);
        assertThat(mids).isNotNull();
        assertThat(toList(mids)).isEqualTo(messageIds);
    }

    @Test
    public void multiMarkWithLabels_shouldSendIntentToCommandService() {
        long uid = 1;
        Set<String> labelToMarkIds = setOf("1", "2", "3");
        Set<String> labelToUnmarkIds = setOf("-1", "-2", "-3");
        List<Long> messageIds = listOf(4L, 5L, 6L);

        List<String> allLabelIds = new ArrayList<>(6);
        allLabelIds.addAll(labelToMarkIds);
        allLabelIds.addAll(labelToUnmarkIds);
        Map<String, Boolean> markMap = new HashMap<>();
        CollectionsKt.forEach(labelToMarkIds, id -> {
            markMap.put(id, true);
            return null;
        });
        CollectionsKt.forEach(labelToUnmarkIds, id -> {
            markMap.put(id, false);
            return null;
        });

        TestObserver<Void> testObserver =
                mailModel.multiMarkWithLabels(uid, labelToMarkIds, labelToUnmarkIds, messageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(3);

        final WorkerInfo offline = startedWorkers.get(0);
        assertThat(offline.getInputString(Constants.ACTION_EXTRA)).isEqualTo(MULTI_MARK_WITH_LABEL_ACTION_OFFLINE);
        assertThat(offline.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(offline.getInputStringArray(LABEL_ID_EXTRAS)).isEqualTo(allLabelIds.toArray(new String[0]));
        assertThat(offline.getInputLongArray(MESSAGE_ID_EXTRAS)).isEqualTo(toLongArray(messageIds));
        assertThat(offline.getInputBoolean(SHOULD_SEND_TO_SERVER, true)).isEqualTo(false);

        final WorkerInfo mark = startedWorkers.get(1);
        assertThat(mark.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MULTI_MARK_MESSAGE_WITH_LABELS_ACTION_API);
        assertThat(mark.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(mark.getInputStringArray(LABEL_ID_EXTRAS)).isEqualTo(labelToMarkIds.toArray(new String[0]));
        assertThat(offline.getInputLongArray(MESSAGE_ID_EXTRAS)).isEqualTo(toLongArray(messageIds));
        assertThat(mark.getInputBoolean(MARK_EXTRAS, false)).isEqualTo(true);

        final WorkerInfo unmark = startedWorkers.get(2);
        assertThat(mark.getInputString(Constants.ACTION_EXTRA)).isEqualTo(CommandsServiceActions.MULTI_MARK_MESSAGE_WITH_LABELS_ACTION_API);
        assertThat(unmark.getInputLong(UID_EXTRA, Constants.NO_UID)).isEqualTo(uid);
        assertThat(unmark.getInputStringArray(LABEL_ID_EXTRAS)).isEqualTo(labelToUnmarkIds.toArray(new String[0]));
        assertThat(offline.getInputLongArray(MESSAGE_ID_EXTRAS)).isEqualTo(toLongArray(messageIds));
        assertThat(unmark.getInputBoolean(MARK_EXTRAS, true)).isEqualTo(false);
    }

    @Test
    public void multiMarkWithLabels_ignoresEmptyMark() {
        long uid = 1;
        Set<String> labelToMarkIds = emptySet();
        Set<String> labelToUnmarkIds = setOf("-1", "-2", "-3");
        List<Long> localMessageIds = listOf(4L, 5L, 6L);

        Map<String, Boolean> markMap = new HashMap<>();
        CollectionsKt.forEach(labelToUnmarkIds, id -> {
            markMap.put(id, false);
            return null;
        });

        TestObserver<Void> testObserver =
                mailModel.multiMarkWithLabels(uid, labelToMarkIds, labelToUnmarkIds, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(2);
        assertThat(CollectionsKt.map(startedWorkers, worker -> worker.getInputBoolean(MARK_EXTRAS, false))).doesNotContain(true);
    }

    @Test
    public void multiMarkWithLabels_ignoresEmptyUnmark() {
        long uid = 1;
        Set<String> labelToMarkIds = setOf("-1", "-2", "-3");
        Set<String> labelToUnmarkIds = emptySet();
        List<Long> localMessageIds = listOf(4L, 5L, 6L);

        Map<String, Boolean> markMap = new HashMap<>();
        CollectionsKt.forEach(labelToMarkIds, id -> {
            markMap.put(id, false);
            return null;
        });

        TestObserver<Void> testObserver =
                mailModel.multiMarkWithLabels(uid, labelToMarkIds, labelToUnmarkIds, localMessageIds).test();

        testObserver.awaitTerminalEvent();
        testObserver.assertNoErrors();

        final List<WorkerInfo> startedWorkers = workerFactory.getAllStartedWorkers();
        assertThat(startedWorkers).hasSize(2);
        assertThat(CollectionsKt.map(startedWorkers, worker -> worker.getInputBoolean(MARK_EXTRAS, true))).doesNotContain(false);
    }

    private void mockImportant(long uid) {
        final AccountComponent accountComponent = mock(AccountComponent.class);
        final LabelsModel labelsModel = mock(LabelsModel.class);

        when(mailApplication.getAccountComponent(uid)).thenReturn(accountComponent);
        when(accountComponent.labelsModel()).thenReturn(labelsModel);
        when(labelsModel.getImportantLabel()).thenReturn(just(createTestLabel(IMPORTANT_LABEL_ID)));
    }
}
