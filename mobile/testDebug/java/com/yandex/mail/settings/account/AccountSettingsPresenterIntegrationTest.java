package com.yandex.mail.settings.account;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.container.AccountInfoContainer;
import com.yandex.mail.data.dms.DMSActionStrings;
import com.yandex.mail.entity.Folder;
import com.yandex.mail.entity.SyncType;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.service.CommandsServiceScheduler;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.settings.AccountSettingsEditor;
import com.yandex.mail.settings.MailSettings.SignaturePlace;
import com.yandex.mail.settings.SimpleStorage;
import com.yandex.mail.settings.SimpleStorageImpl;
import com.yandex.mail.storage.StubSharedPreferences;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.ui.presenters.configs.AccountPresenterConfig;
import com.yandex.mail.util.BaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.service.work.InputDataUtilsKt.getAction;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(IntegrationTestRunner.class)
public class AccountSettingsPresenterIntegrationTest extends BaseIntegrationTest {

    private SimpleStorage simpleStorage = new SimpleStorageImpl(new StubSharedPreferences(), mock(YandexMailMetrica.class));

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private AccountSettingsPresenter presenter;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    @Mock
    private AccountSettingsEditor editorMock;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    @Mock
    private AccountSettings accountSettingsMock;

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    @Mock
    private AccountSettingsView accountSettingsView;

    @Before
    public void setUp() throws Exception {
        CommandsServiceScheduler.INSTANCE.observeTaskRequestsAndSubmitAllTasks(app);
    }

    @Test
    public void loadLoggedAccount_shouldStartLoadingOfSettingsIfNotLoaded() {
        initNotLoaded();
        settingsModel.accountSettings(user.getUid()).edit().setSyncedWithServer(false).commitAndSync();

        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_SETTINGS.equals(getAction(data)))).isEmpty();

        shadowOf(getMainLooper()).idle();
        presenter.loadAccount();
        shadowOf(getMainLooper()).idle();

        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_SETTINGS.equals(getAction(data)))).hasSize(1);
    }

    @Test
    public void loadLoggedAccount_shouldNotStartLoadingOfSettingsIfLoaded() {
        initLoaded();

        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_SETTINGS.equals(getAction(data)))).isEmpty();

        presenter.loadAccount();

        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_SETTINGS.equals(getAction(data)))).isEmpty();
    }

    @Test
    public void loadFolderInfoList_shouldStartLoadingOfContainersIfNotLoaded() {
        initNotLoaded();
        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_CONTAINERS.equals(getAction(data)))).isEmpty();

        shadowOf(getMainLooper()).idle();
        presenter.loadFolderAndTabsInfoList();
        shadowOf(getMainLooper()).idle();

        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_CONTAINERS.equals(getAction(data)))).hasSize(1);
    }

    @Test
    public void loadFolderInfoList_shouldNotStartLoadingOfContainersIfLoaded() {
        initLoaded();
        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_CONTAINERS.equals(getAction(data)))).isEmpty();

        presenter.loadFolderAndTabsInfoList();

        assertThat(dataManagingExecutor.getWorks(data -> DMSActionStrings.LOAD_CONTAINERS.equals(getAction(data)))).isEmpty();
    }

    @Test
    public void putFolderSyncNotifyPreference_shouldSaveDoNotSyncPreferenceAfterSaveSettings() {
        initLoaded();
        final Folder folder = foldersModel.getFolderByFidFromCache(inboxFid()).blockingGet().get();

        presenter.putFolderSyncNotifyPreference(folder.getFid(), SyncType.DO_NOT_SYNC);
        presenter.saveSettings();
        assertThat(foldersModel.getFolderSyncType(inboxFid()).blockingGet().get()).isEqualTo(SyncType.DO_NOT_SYNC);
    }

    @Test
    public void putFolderSyncNotifyPreference_shouldSaveSyncSilentPreferenceAfterSaveSettings() {
        initLoaded();
        final Folder folder = foldersModel.getFolderByFidFromCache(inboxFid()).blockingGet().get();

        presenter.putFolderSyncNotifyPreference(folder.getFid(), SyncType.SYNC_SILENT);
        presenter.saveSettings();
        assertThat(foldersModel.getFolderSyncType(inboxFid()).blockingGet().get()).isEqualTo(SyncType.SYNC_SILENT);
    }

    @Test
    public void putFolderSyncNotifyPreference_shouldSaveSyncAndPushPreferenceAfterSaveSettings() {
        initLoaded();
        final Folder folder = foldersModel.getFolderByFidFromCache(inboxFid()).blockingGet().get();

        presenter.putFolderSyncNotifyPreference(folder.getFid(), SyncType.SYNC_AND_PUSH);
        presenter.saveSettings();
        assertThat(foldersModel.getFolderSyncType(inboxFid()).blockingGet().get()).isEqualTo(SyncType.SYNC_AND_PUSH);
    }

    @Test
    public void putIsUsedInAppState_shouldSaveIsUsedInAppStateAfterSaveSettings() {
        initLoaded();
        presenter.loadAccount();
        presenter.putIsUsedInApp(false);
        presenter.saveSettings();

        AccountInfoContainer accountInfo = CollectionsKt.first(accountModel.getAccountsInfo().blockingGet());
        assertThat(accountInfo.getUsedInApp()).isFalse();
    }

    @Test
    public void putIsUsedInApp_shouldSelectNewActiveAccount() {
        FakeServer.getInstance().createAccountWrapper(Accounts.teamLoginData);
        User user1 = User.create(Accounts.teamLoginData);
        user1.initialLoad();

        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        User user2 = User.create(Accounts.testLoginData);
        user2.initialLoad();

        applicationComponent = BaseMailApplication.getApplicationComponent(IntegrationTestRunner.app());
        accountComponent = BaseMailApplication.getAccountComponent(IntegrationTestRunner.app(), user2.getUid());

        initModels(applicationComponent, accountComponent);
        accountModel.selectAccount(user2.getUid());

        final AccountSettingsPresenter presenter = createAccountSettingsPresenter(createTestPresenterConfig(user2.getUid()));
        presenter.loadAccount();
        presenter.putIsUsedInApp(false);
        presenter.saveSettings();

        final List<AccountInfoContainer> accountsInfo = accountModel.getAccountsInfo().blockingGet();

        assertThat(accountsInfo).extracting(accountInfo -> new Pair<>(accountInfo.getId(), accountInfo.getSelected()))
                .containsOnly(new Pair<>(user1.getUid(), true), new Pair<>(user2.getUid(), false));
    }

    @Test
    public void putSignaturePreference_shouldSetSignaturePlaceToNoneIfSignatureIsEmpty() {
        initLoaded();
        presenter.loadAccount();

        presenter.putSignaturePreference("Non empty");
        presenter.putSignaturePlacePreference(SignaturePlace.AT_THE_END);
        presenter.saveSettings();

        presenter.putSignaturePreference("");
        presenter.saveSettings();

        assertThat(accountSettings.signaturePlace()).isEqualTo(SignaturePlace.NONE);
    }

    @Test
    public void putSignaturePreference_shouldSetSignaturePlaceToAtTheEndIfSignatureIsNotEmpty() {
        initLoaded();
        presenter.loadAccount();

        presenter.putSignaturePlacePreference(SignaturePlace.NONE);
        presenter.saveSettings();

        presenter.putSignaturePreference("Non Empty");
        presenter.saveSettings();

        assertThat(accountSettings.signaturePlace()).isEqualTo(SignaturePlace.AT_THE_END);
    }

    @Test
    public void saveSettings_shouldCallCommitOnEditor() {
        initForMocks();
        foldersModel = mock(FoldersModel.class);
        when(foldersModel.updateFoldersSyncType(any())).thenReturn(Completable.complete());

        presenter.onBindView(accountSettingsView);
        presenter.loadAccount();
        presenter.saveSettings();
        verify(editorMock).commitAndSync();
    }

    private void initForMocks() {
        MockitoAnnotations.initMocks(this);
        init(Accounts.testLoginData, true);
        accountSettings.edit().setThreadModeEnabled(true).commitAndSync();

        AccountPresenterConfig presenterConfig = createTestPresenterConfig(Accounts.testLoginData.uid);
        presenter = new AccountSettingsPresenter(
                IntegrationTestRunner.app(),
                accountModel,
                accountSettingsMock,
                foldersModel,
                notificationsModel,
                presenterConfig,
                simpleStorage
        );
        when(accountSettingsMock.syncedWithServer()).thenReturn(true);
        when(accountSettingsMock.edit()).thenReturn(editorMock);
        when(accountSettingsMock.editAndSync()).thenReturn(editorMock);
    }

    private void initLoaded() {
        init(Accounts.testLoginData, true);
        accountSettings.edit().setThreadModeEnabled(true).commitAndSync();

        AccountPresenterConfig presenterConfig = createTestPresenterConfig(Accounts.testLoginData.uid);
        presenter = createAccountSettingsPresenter(presenterConfig);
        shadowOf(getMainLooper()).idle();
    }

    private void initNotLoaded() {
        init(Accounts.testLoginData, false);
        accountSettings.edit().setThreadModeEnabled(true).commitAndSync();

        final AccountPresenterConfig presenterConfig = createTestPresenterConfig(Accounts.testLoginData.uid);
        presenter = createAccountSettingsPresenter(presenterConfig);
        shadowOf(getMainLooper()).idle();
    }

    @NonNull
    private AccountPresenterConfig createTestPresenterConfig(long uid) {
        return new AccountPresenterConfig(
                trampoline(),
                trampoline(),
                uid
        );
    }

    @NonNull
    private AccountSettingsPresenter createAccountSettingsPresenter(@NonNull AccountPresenterConfig presenterConfig) {
        return new AccountSettingsPresenter(
                IntegrationTestRunner.app(),
                accountModel,
                accountSettings,
                foldersModel,
                notificationsModel,
                presenterConfig,
                simpleStorage
        );
    }
}
