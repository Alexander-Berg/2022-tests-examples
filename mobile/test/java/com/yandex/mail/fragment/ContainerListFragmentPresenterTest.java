package com.yandex.mail.fragment;

import com.yandex.mail.TestMailApplication;
import com.yandex.mail.containers_list.ContainerListFragmentPresenter;
import com.yandex.mail.containers_list.ContainerListFragmentPresenterSettings;
import com.yandex.mail.containers_list.ContainerListFragmentView;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.entity.AccountEntity;
import com.yandex.mail.entity.AccountType;
import com.yandex.mail.entity.FidWithCounters;
import com.yandex.mail.entity.Label;
import com.yandex.mail.entity.NanoFoldersTree;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.CrossAccountModel;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.LabelsModel;
import com.yandex.mail.model.NewsLettersModel;
import com.yandex.mail.notifications.NotificationsModel;
import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.settings.GeneralSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.storage.entities.EntitiesTestFactory.buildNanoFolder;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static java.util.Collections.emptyList;
import static kotlin.collections.CollectionsKt.listOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(UnitTestRunner.class)
public class ContainerListFragmentPresenterTest {

    @NonNull
    private ContainerListFragmentPresenter presenter;

    @Mock
    @NonNull
    TestMailApplication mockApplication;

    @Mock
    @NonNull
    FoldersModel mockFolderModel;

    @Mock
    @NonNull
    LabelsModel mockLabelModel;

    @Mock
    @NonNull
    AccountModel accountModel;

    @Mock
    @NonNull
    CrossAccountModel crossAccountModel;

    @Mock
    @NonNull
    NotificationsModel notificationsModel;

    @Mock
    @NonNull
    AccountComponent mockAccountComponent;

    @Mock
    @NonNull
    AccountSettings mockAccountSettings;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    @NonNull
    GeneralSettings generalSettings;

    @Mock
    @NonNull
    NewsLettersModel newsLettersModel;

    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);

        when(mockApplication.getAccountComponent(anyLong())).thenReturn(mockAccountComponent);
        when(mockAccountComponent.foldersModel()).thenReturn(mockFolderModel);
        when(mockAccountComponent.labelsModel()).thenReturn(mockLabelModel);
        when(mockAccountComponent.settings()).thenReturn(mockAccountSettings);
        when(mockAccountComponent.newsLetterModel()).thenReturn(newsLettersModel);
        when(mockAccountComponent.accountType()).thenReturn(AccountType.LOGIN);

        AccountEntity account = mock(AccountEntity.class);
        when(account.getUid()).thenReturn(1L);
        when(account.getHasToken()).thenReturn(true);
        when(accountModel.observeSelectedAccount()).thenReturn(Flowable.just(Optional.of(account)));
        when(crossAccountModel.observeUboxWithEmails()).thenReturn(Flowable.just(emptyList()));
        when(newsLettersModel.observeOptInCounter()).thenReturn(Observable.just(0L));

        presenter = new ContainerListFragmentPresenter(
                mockApplication,
                accountModel,
                crossAccountModel,
                notificationsModel,
                generalSettings
        );
    }

    @Test
    public void onAccountChanged_subscribesToLatestData() {
        ContainerListFragmentView mockView = mock(ContainerListFragmentView.class);

        PublishSubject<Boolean> threadModeObservable = PublishSubject.create();

        PublishProcessor<List<Label>> labelsPublish = PublishProcessor.create();
        List<Label> labels1 = mock(List.class);
        List<Label> labels2 = mock(List.class);

        PublishProcessor<NanoFoldersTree> foldersObservable = PublishProcessor.create();
        NanoFoldersTree folders1 = new NanoFoldersTree(mockApplication, listOf(buildNanoFolder()), Collections.emptyMap());
        NanoFoldersTree folders2 = new NanoFoldersTree(mockApplication, listOf(buildNanoFolder()), Collections.emptyMap());

        PublishSubject<Map<Long, Boolean>> folderExpandedObservable = PublishSubject.create();
        Map<Long, Boolean> foldersExpand1 = mock(Map.class);
        Map<Long, Boolean> foldersExpand2 = mock(Map.class);

        PublishProcessor<Map<Long, FidWithCounters>> folderCountersObservable = PublishProcessor.create();
        Map<Long, FidWithCounters> folderCounters1 = mock(Map.class);
        Map<Long, FidWithCounters> folderCounters2 = mock(Map.class);

        when(mockFolderModel.observeFoldersTree()).thenReturn(foldersObservable);
        when(mockFolderModel.observeCounters()).thenReturn(folderCountersObservable);
        when(mockLabelModel.observeLabels()).thenReturn(labelsPublish);
        presenter.onBindView(mockView);
        presenter.onResume();
        shadowOf(getMainLooper()).idle();
        clearInvocations(mockView);
        shadowOf(getMainLooper()).idle();

        threadModeObservable.onNext(true);
        shadowOf(getMainLooper()).idle();
        verifyNoInteractions(mockView);

        foldersObservable.onNext(folders1);
        shadowOf(getMainLooper()).idle();
        verifyNoInteractions(mockView);

        folderExpandedObservable.onNext(foldersExpand1);
        shadowOf(getMainLooper()).idle();
        verifyNoInteractions(mockView);

        folderCountersObservable.onNext(folderCounters1);
        shadowOf(getMainLooper()).idle();
        verifyNoInteractions(mockView);

        labelsPublish.onNext(labels1);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders1), eq(folderCounters1), eq(labels1), eq(AccountType.LOGIN), any(), eq(0L), any());

        threadModeObservable.onNext(false);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders1), eq(folderCounters1), eq(labels1), eq(AccountType.LOGIN), any(), eq(0L), any());

        labelsPublish.onNext(labels2);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders1), eq(folderCounters1), eq(labels2), eq(AccountType.LOGIN), any(), eq(0L), any());

        foldersObservable.onNext(folders2);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders2), eq(folderCounters1), eq(labels2), eq(AccountType.LOGIN), any(), eq(0L), any());

        folderExpandedObservable.onNext(foldersExpand2);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders2), eq(folderCounters1), eq(labels2), eq(AccountType.LOGIN), any(), eq(0L), any());

        folderCountersObservable.onNext(folderCounters2);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders2), eq(folderCounters2), eq(labels2), eq(AccountType.LOGIN), any(), eq(0L), any());
    }

    @Test
    public void onAccountChanged_subscribesToValidAccountSettings() {
        ContainerListFragmentView mockView = mock(ContainerListFragmentView.class);

        PublishSubject<Boolean> threadModeObservable = PublishSubject.create();

        PublishProcessor<List<Label>> labelsPublish = PublishProcessor.create();
        List<Label> labels = listOf(mock(Label.class));

        PublishProcessor<NanoFoldersTree> foldersObservable = PublishProcessor.create();
        NanoFoldersTree folders = new NanoFoldersTree(mockApplication, listOf(buildNanoFolder()), Collections.emptyMap());

        PublishProcessor<Map<Long, FidWithCounters>> folderCountersObservable = PublishProcessor.create();
        Map<Long, FidWithCounters> folderCounters = mock(Map.class);

        when(mockFolderModel.observeFoldersTree()).thenReturn(foldersObservable);
        when(mockFolderModel.observeCounters()).thenReturn(folderCountersObservable);
        when(mockLabelModel.observeLabels()).thenReturn(labelsPublish);

        presenter.onBindView(mockView);
        presenter.onResume();
        shadowOf(getMainLooper()).idle();

        labelsPublish.onNext(labels);
        foldersObservable.onNext(folders);
        folderCountersObservable.onNext(folderCounters);
        threadModeObservable.onNext(true);
        shadowOf(getMainLooper()).idle();

        verify(mockView).onDataLoaded(eq(folders), eq(folderCounters), eq(labels), eq(AccountType.LOGIN), any(), eq(0L), any());
    }

    @Test
    public void onAccountChanged_subscribesToValidLabels() {
        ContainerListFragmentView mockView = mock(ContainerListFragmentView.class);

        PublishSubject<Boolean> threadModeObservable = PublishSubject.create();

        PublishProcessor<List<Label>> labelsPublish = PublishProcessor.create();
        List<Label> labels = listOf(mock(Label.class));

        PublishProcessor<NanoFoldersTree> foldersObservable = PublishProcessor.create();
        NanoFoldersTree folders = new NanoFoldersTree(mockApplication, listOf(buildNanoFolder()), Collections.emptyMap());

        PublishProcessor<Map<Long, FidWithCounters>> folderCountersObservable = PublishProcessor.create();
        Map<Long, FidWithCounters> folderCounters = mock(Map.class);

        when(mockFolderModel.observeFoldersTree()).thenReturn(foldersObservable);
        when(mockFolderModel.observeCounters()).thenReturn(folderCountersObservable);
        when(mockLabelModel.observeLabels()).thenReturn(labelsPublish);

        presenter.onBindView(mockView);
        presenter.onResume();
        shadowOf(getMainLooper()).idle();

        threadModeObservable.onNext(false);
        labelsPublish.onNext(emptyList());
        foldersObservable.onNext(folders);
        folderCountersObservable.onNext(folderCounters);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders), eq(folderCounters), eq(emptyList()), eq(AccountType.LOGIN), any(), eq(0L), any()); // with empty dafalseta
        clearInvocations(mockView);

        labelsPublish.onNext(labels);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders), eq(folderCounters), eq(labels), eq(AccountType.LOGIN), any(), eq(0L), any());
    }

    @Test
    public void onAccountChanged_shouldCallOnDataLoadedIfFoldersOrLabelsIsEmpty() {
        ContainerListFragmentView mockView = mock(ContainerListFragmentView.class);

        PublishSubject<Boolean> threadModeObservable = PublishSubject.create();

        PublishProcessor<List<Label>> labelsPublish = PublishProcessor.create();
        List<Label> labels = listOf(mock(Label.class));

        PublishProcessor<NanoFoldersTree> foldersObservable = PublishProcessor.create();
        NanoFoldersTree folders = new NanoFoldersTree(mockApplication, emptyList(), Collections.emptyMap());

        PublishProcessor<Map<Long, FidWithCounters>> folderCountersObservable = PublishProcessor.create();
        Map<Long, FidWithCounters> folderCounters = mock(Map.class);

        when(mockFolderModel.observeFoldersTree()).thenReturn(foldersObservable);
        when(mockFolderModel.observeCounters()).thenReturn(folderCountersObservable);
        when(mockLabelModel.observeLabels()).thenReturn(labelsPublish);

        presenter.onBindView(mockView);
        presenter.onResume();
        shadowOf(getMainLooper()).idle();

        labelsPublish.onNext(labels);
        foldersObservable.onNext(folders);
        folderCountersObservable.onNext(folderCounters);
        threadModeObservable.onNext(true);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders), eq(folderCounters), eq(labels), eq(AccountType.LOGIN), any(), eq(0L), any());
        Mockito.clearInvocations(mockView);

        labels = emptyList();
        labelsPublish.onNext(labels);
        shadowOf(getMainLooper()).idle();
        verify(mockView).onDataLoaded(eq(folders), eq(folderCounters), eq(labels), eq(AccountType.LOGIN), any(), eq(0L), any());
    }

    @Test
    public void onBindView_subscribesToSelectedAccount() {
        ContainerListFragmentView mockView = mock(ContainerListFragmentView.class);

        PublishProcessor<Optional<AccountEntity>> accountSubject = PublishProcessor.create();
        when(accountModel.observeSelectedAccount()).thenReturn(accountSubject);
        when(mockFolderModel.observeFoldersTree()).thenReturn(Flowable.empty());
        when(mockFolderModel.observeCounters()).thenReturn(Flowable.empty());
        when(mockLabelModel.observeLabels()).thenReturn(Flowable.empty());

        presenter = new ContainerListFragmentPresenter(
                mockApplication,
                accountModel,
                crossAccountModel,
                notificationsModel,
                generalSettings
        );

        presenter.onBindView(mockView);
        shadowOf(getMainLooper()).idle();

        AccountEntity account1 = mock(AccountEntity.class);
        when(account1.getUid()).thenReturn(1L);
        when(account1.getHasToken()).thenReturn(true);
        accountSubject.onNext(Optional.of(account1));
        shadowOf(getMainLooper()).idle();
        verify(mockView).onAccountChanged(account1.getUid());

        AccountEntity account2 = mock(AccountEntity.class);
        when(account2.getUid()).thenReturn(2L);
        when(account2.getHasToken()).thenReturn(true);
        accountSubject.onNext(Optional.of(account2));
        shadowOf(getMainLooper()).idle();
        verify(mockView).onAccountChanged(account2.getUid());
    }

    @Test
    public void onAccountChanged_recreatesNewModels() {
        ContainerListFragmentView mockView = mock(ContainerListFragmentView.class);

        PublishProcessor<Optional<AccountEntity>> accountSubject = PublishProcessor.create();
        when(accountModel.observeSelectedAccount()).thenReturn(accountSubject);
        when(mockFolderModel.observeFoldersTree()).thenReturn(Flowable.empty());
        when(mockFolderModel.observeCounters()).thenReturn(Flowable.empty());
        when(mockLabelModel.observeLabels()).thenReturn(Flowable.empty());

        presenter = new ContainerListFragmentPresenter(
                mockApplication,
                accountModel,
                crossAccountModel,
                notificationsModel,
                generalSettings
        );

        presenter.onBindView(mockView);
        shadowOf(getMainLooper()).idle();

        AccountEntity account1 = mock(AccountEntity.class);
        when(account1.getUid()).thenReturn(1L);
        when(account1.getHasToken()).thenReturn(true);
        accountSubject.onNext(Optional.of(account1));
        shadowOf(getMainLooper()).idle();
        verify(mockApplication).getAccountComponent(account1.getUid());
        verify(mockAccountComponent).foldersModel();
        verify(mockAccountComponent).labelsModel();
        //noinspection CheckResult

        AccountEntity account2 = mock(AccountEntity.class);
        when(account2.getUid()).thenReturn(2L);
        when(account2.getHasToken()).thenReturn(true);
        accountSubject.onNext(Optional.of(account2));
        shadowOf(getMainLooper()).idle();
        verify(mockApplication).getAccountComponent(account2.getUid());
        verify(mockAccountComponent, times(2)).foldersModel();
        verify(mockAccountComponent, times(2)).labelsModel();
        //noinspection CheckResult
    }

    private static class TestContainerListFragmentPresenterSettings extends ContainerListFragmentPresenterSettings {

        @NonNull
        @Override
        public Scheduler getIoScheduler() {
            return trampoline();
        }
    }
}
