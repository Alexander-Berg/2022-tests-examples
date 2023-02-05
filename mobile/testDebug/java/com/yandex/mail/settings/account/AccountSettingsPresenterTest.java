package com.yandex.mail.settings.account;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.account.MailProvider;
import com.yandex.mail.container.AccountInfoContainer;
import com.yandex.mail.di.ApplicationComponent;
import com.yandex.mail.entity.AccountType;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.NanoFoldersTree;
import com.yandex.mail.entity.SyncType;
import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.notifications.NotificationsModel;
import com.yandex.mail.runners.UnitTestRunner;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.settings.AccountSettingsEditor;
import com.yandex.mail.settings.SimpleStorage;
import com.yandex.mail.settings.account.AccountSettingsView.FolderUIInfo;
import com.yandex.mail.ui.presenters.configs.AccountPresenterConfig;
import com.yandex.mail.util.NanomailEntitiesTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Single;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;

import static com.yandex.mail.settings.MailSettings.SignaturePlace.AT_THE_END;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static kotlin.collections.CollectionsKt.emptyList;
import static kotlin.collections.CollectionsKt.listOf;
import static kotlin.collections.MapsKt.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(UnitTestRunner.class)
public class AccountSettingsPresenterTest {

    private static final int UID = 1;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private BaseMailApplication baseMailApplication;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private AccountModel accountModel;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private FoldersModel foldersModel;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private NotificationsModel notificationsModel;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    @Mock
    private NanoFoldersTree foldersTree;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    @Mock
    private AccountSettings accountSettings;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    @Mock
    private AccountSettingsEditor editor;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    @Mock
    private AccountSettingsView accountSettingsView;

    @SuppressWarnings("NullableProblems") // @Before
    @Captor
    @NonNull
    private ArgumentCaptor<List<FolderUIInfo>> foldersCaptor;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountSettingsPresenter presenter;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountPresenterConfig presenterConfig;

    @NonNull
    private final AccountInfoContainer mainContainer =
            AccountInfoContainer.create(
                    1,
                    "m_name",
                    "m_type",
                    true,
                    true,
                    true,
                    AccountType.LOGIN,
                    MailProvider.YANDEX,
                    false,
                    "name",
                    "email",
                    true
            );

    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);

        presenterConfig = createTestPresenterConfig();
        presenter = createAccountSettingsPresenter();
        mockGetAccountsInfoWithMails(1);
        when(accountSettings.syncedWithServer()).thenReturn(true);
        when(accountSettings.edit()).thenReturn(editor);
        when(accountSettings.editAndSync()).thenReturn(editor);
        when(baseMailApplication.getString(anyInt())).thenAnswer(
                invocation -> RuntimeEnvironment.application.getString(invocation.getArgument(0))
        );
    }

    @Test
    public void loadSettings_shouldInvokeOnInitialSettingsLoadedDisabledOnSingletonList() {
        mockGetAccountsInfoWithMails(1);

        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();
        verify(accountSettingsView).onInitialSettingsLoaded(mainContainer, accountSettings, false);
    }

    @Test
    public void loadSettings_shouldInvokeOnInitialSettingsLoadedEnabledOnNonSingletonList() {
        mockGetAccountsInfoWithMails(3);

        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();
        verify(accountSettingsView).onInitialSettingsLoaded(mainContainer, accountSettings, true);
    }

    @Test
    public void loadFolderAndTabsInfoList_shouldInvokeOnFoldersListWithDepthLoaded() {
        final Folder testFolder1 = NanomailEntitiesTestUtils.createTestFolder(1);
        final Folder parentFolder = testFolder1.copy(
                testFolder1.getFid(),
                testFolder1.getType(),
                "parent",
                testFolder1.getPosition(),
                testFolder1.getParent(),
                testFolder1.getUnread_counter(),
                testFolder1.getTotal_counter()
        );
        final Folder testFolder2 = NanomailEntitiesTestUtils.createTestFolder(2);
        final Folder childFolder = testFolder2.copy(
                testFolder2.getFid(),
                testFolder2.getType(),
                "parent|child",
                testFolder2.getPosition(),
                testFolder2.getParent(),
                testFolder2.getUnread_counter(),
                testFolder2.getTotal_counter()
        );

        List<Folder> initialList = listOf(parentFolder, childFolder);
        Map<Long, SyncType> syncTypeMap = CollectionsKt.associateBy(initialList, Folder::getFid, f -> SyncType.SYNC_AND_PUSH);

        presenter = createAccountSettingsPresenter();

        when(foldersModel.observeFoldersTree()).thenReturn(Flowable.just(foldersTree));
        when(foldersModel.getAllFoldersSyncType()).thenReturn(Single.just(syncTypeMap));
        when(foldersTree.size()).thenReturn(2);
        when(foldersTree.getSortedFolders()).thenReturn(initialList);
        when(foldersTree.getDepth(parentFolder)).thenReturn(0);
        when(foldersTree.getDepth(childFolder)).thenReturn(1);
        when(foldersTree.getLocaleAwareName(parentFolder)).thenReturn("parent");
        when(foldersTree.getLocaleAwareName(childFolder)).thenReturn("child");

        presenter.onBindView(accountSettingsView);
        presenter.loadFolderAndTabsInfoList();

        verify(accountSettingsView).onFoldersInfoLoaded(foldersCaptor.capture());

        List<FolderUIInfo> folders = foldersCaptor.getValue();
        assertThat(folders).hasSize(2);
        assertThat(folders.get(0).getTitle()).isEqualTo("parent");
        assertThat(folders.get(1).getTitle()).isEqualTo("child");
    }

    @Test
    public void loadFolderAndTabsInfoList_shouldReturnRightSyncTypesForTabs() {
        Map<Long, SyncType> syncTypeMap = MapsKt.emptyMap();

        when(foldersModel.observeFoldersTree()).thenReturn(Flowable.just(foldersTree));
        when(foldersModel.getAllFoldersSyncType()).thenReturn(io.reactivex.Single.just(syncTypeMap));
        when(foldersTree.size()).thenReturn(2);
        when(foldersTree.getSortedFolders()).thenReturn(emptyList());

        presenter.onBindView(accountSettingsView);
        presenter.loadFolderAndTabsInfoList();

        verify(accountSettingsView).onFoldersInfoLoaded(foldersCaptor.capture());
    }

    @Test
    public void loadFolderAndTabsInfoList_shouldExcludeInboxFolders_ifTabsAreEnabled() {
        Map<Long, SyncType> syncTypeMap = emptyMap();

        when(foldersModel.observeFoldersTree()).thenReturn(Flowable.just(foldersTree));
        when(foldersModel.getAllFoldersSyncType()).thenReturn(io.reactivex.Single.just(syncTypeMap));
        when(foldersTree.size()).thenReturn(2);
        when(foldersTree.getSortedFolders()).thenReturn(emptyList());

        presenter.onBindView(accountSettingsView);
        presenter.loadFolderAndTabsInfoList();

        verify(accountSettingsView).onFoldersInfoLoaded(foldersCaptor.capture());

        List<FolderUIInfo> folders = foldersCaptor.getValue();
        assertThat(folders).hasSize(0);
    }

    @Test
    public void calendarPreferenceTest() {
        when(editor.setCalendarInterval(30)).thenReturn(editor);
        ApplicationComponent applicationComponent = mock(ApplicationComponent.class);
        when(applicationComponent.metrica()).thenReturn(mock(YandexMailMetrica.class));
        when(baseMailApplication.getApplicationComponent()).thenReturn(applicationComponent);
        when(baseMailApplication.getApplicationContext()).thenReturn(baseMailApplication);


        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();
        presenter.putCalendarIntervalPreference(30);

        verify(editor).apply();
    }

    @Test
    public void putSignaturePreference_shouldCall_onSignaturePlaceChanged() {
        when(editor.setSignature(any())).thenReturn(editor);
        when(editor.setUseDefaultSignature(anyBoolean())).thenReturn(editor);
        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();

        when(accountSettings.signaturePlace()).thenReturn(AT_THE_END);
        presenter.putSignaturePreference("Place");

        verify(accountSettingsView).onSignaturePlaceChanged(AT_THE_END);
    }

    @Test
    public void putSignaturePreference_shouldNotSetUseDefaultSignature() {
        when(editor.setSignature(any())).thenReturn(editor);
        when(editor.setUseDefaultSignature(anyBoolean())).thenReturn(editor);
        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();

        when(accountSettings.signaturePlace()).thenReturn(AT_THE_END);
        presenter.putSignaturePreference("Place");

        verify(editor, never()).setUseDefaultSignature(anyBoolean());
    }

    @Test
    public void putSignaturePlacePreference_should_call_onSignaturePlaceChanged() {
        when(editor.setSignaturePlace(any())).thenReturn(editor);
        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();

        when(accountSettings.signaturePlace()).thenReturn(AT_THE_END);
        presenter.putSignaturePlacePreference(AT_THE_END);

        verify(accountSettingsView).onSignaturePlaceChanged(AT_THE_END);
    }

    private void mockGetAccountsInfoWithMails(int amount) {
        ArrayList<AccountInfoContainer> containers = new ArrayList<>();
        containers.add(mainContainer);
        for (int i = 1; i < amount; i++) {
            containers.add(
                    AccountInfoContainer.create(
                            i + 1,
                            "m_name",
                            "m_type",
                            true,
                            false,
                            true,
                            AccountType.LOGIN,
                            MailProvider.YANDEX,
                            false,
                            "name",
                            "email",
                            true
                    )
            );
        }
        when(accountModel.getAccountsInfoWithMails()).thenReturn(Single.just(containers));
    }

    @NonNull
    private AccountPresenterConfig createTestPresenterConfig() {
        return new AccountPresenterConfig(
                trampoline(),
                trampoline(),
                UID
        );
    }

    @NonNull
    private AccountSettingsPresenter createAccountSettingsPresenter() {
        return new AccountSettingsPresenter(
                baseMailApplication,
                accountModel,
                accountSettings,
                foldersModel,
                notificationsModel,
                presenterConfig,
                mock(SimpleStorage.class)
        );
    }
}
