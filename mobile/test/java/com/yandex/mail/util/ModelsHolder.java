package com.yandex.mail.util;

import com.google.gson.Gson;
import com.pushtorefresh.storio3.sqlite.StorIOSQLite;
import com.squareup.sqldelight.Transacter;
import com.yandex.mail.am.MockPassportApi;
import com.yandex.mail.attach.AttachmentPreviewModel;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.AccountComponentProvider;
import com.yandex.mail.di.ApplicationComponent;
import com.yandex.mail.metrica.MockYandexMailMetricaModule.TestYandexMailMetrica;
import com.yandex.mail.model.AbookModel;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.AddressModel;
import com.yandex.mail.model.AttachmentsModel;
import com.yandex.mail.model.AuthModel;
import com.yandex.mail.model.CleanupModel;
import com.yandex.mail.model.ComposeStoreModel;
import com.yandex.mail.model.CrossAccountModel;
import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.model.DraftAttachmentsModel;
import com.yandex.mail.model.DraftsModel;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.GeneralSettingsModel;
import com.yandex.mail.model.LabelsModel;
import com.yandex.mail.model.MessagesModel;
import com.yandex.mail.model.NameAlternativesModel;
import com.yandex.mail.model.NewsLettersModel;
import com.yandex.mail.model.SearchModel;
import com.yandex.mail.model.SearchSuggestsModel;
import com.yandex.mail.model.SettingsModel;
import com.yandex.mail.model.SyncModel;
import com.yandex.mail.model.ThreadsModel;
import com.yandex.mail.model.TranslatorModel;
import com.yandex.mail.network.MailApi;
import com.yandex.mail.network.UnauthorizedMailApi;
import com.yandex.mail.notifications.ChannelSynchronizer;
import com.yandex.mail.notifications.NotificationsModel;
import com.yandex.mail.pin.PinCodeModel;
import com.yandex.mail.pin.PinState;
import com.yandex.mail.push.MailMessagingServiceDelegate;
import com.yandex.mail.service.work.TestDataManagingExecutor;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.settings.GeneralSettings;
import com.yandex.mail.settings.SimpleStorage;
import com.yandex.mail.ui.presenters.promos.UnsubscribeTipStrategy;
import com.yandex.mail.widget.MailWidgetsModel;

import androidx.annotation.NonNull;

public class ModelsHolder {

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public AuthModel authModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public AccountModel accountModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public MockPassportApi passportApi;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public GeneralSettingsModel settingsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public NotificationsModel notificationsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public TestYandexMailMetrica metrica;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public ActionTimeTracker actionTimeTracker;

    @NonNull
    public com.yandex.xplat.xmail.ActionTimeTracker xmailActionTimeTracker;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public CountingTracker countingTracker;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public UnauthorizedMailApi unauthorizedMailApi;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public GeneralSettings generalSettings;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public NewYearModel newYearModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public PinCodeModel pinCodeModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public PinState pinState;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public MessagesModel messagesModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public ComposeStoreModel composeStoreModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public FoldersModel foldersModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public ThreadsModel threadsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public LabelsModel labelsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public SearchModel searchModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public SearchSuggestsModel searchSuggestsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public NameAlternativesModel nameAlternativesModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public AttachmentsModel attachmentsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public AttachmentPreviewModel attachmentPreviewModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public DraftAttachmentsModel draftAttachmentsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public SyncModel syncModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public Transacter transacter;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public StorIOSQLite ftsSQLite;

    @SuppressWarnings("NullableProblems") // @Before.
    @NonNull
    public AbookModel abookModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public DraftsModel draftsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public AccountSettings accountSettings;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public MailApi mailApi;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public SettingsModel nanoSettingsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public Gson gson;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public CleanupModel cleanupModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public DeveloperSettingsModel developerSettingsModel;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public SimpleStorage simpleStorage;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public MailWidgetsModel widgetsModel;

    @SuppressWarnings("NullableProblems")
    @NonNull
    public ChannelSynchronizer channelSynchronizer;

    @SuppressWarnings("NullableProblems") // initModels
    @NonNull
    public TranslatorModel translatorModel;

    @NonNull
    public MailMessagingServiceDelegate mailMessagingService;

    @NonNull
    public AddressModel addressModel;

    @NonNull
    public CrossAccountModel crossAccountModel;

    @NonNull
    public UnsubscribeTipStrategy unsubscribeTipStrategy;

    @NonNull
    public NewsLettersModel newsLettersModel;

    @NonNull
    public AccountComponentProvider accountComponentProvider;

    @NonNull
    public TestDataManagingExecutor dataManagingExecutor;

    public void initModels(@NonNull ApplicationComponent applicationComponent, @NonNull AccountComponent accountComponent) {
        initApplicationModels(applicationComponent);
        initAccountModels(accountComponent, false);
    }

    public void initApplicationModels(@NonNull ApplicationComponent applicationComponent) {
        authModel = applicationComponent.authModel();
        accountModel = applicationComponent.accountModel();
        passportApi = (MockPassportApi) applicationComponent.passportApi();
        settingsModel = applicationComponent.settingsModel();
        notificationsModel = applicationComponent.notificationsModel();
        metrica = (TestYandexMailMetrica) applicationComponent.metrica();
        actionTimeTracker = applicationComponent.actionTimeTracker();
        xmailActionTimeTracker = applicationComponent.xmailActionTimeTracker();
        countingTracker = applicationComponent.countingTracker();
        unauthorizedMailApi = applicationComponent.unauthorizedMailApi();
        generalSettings = applicationComponent.generalSettings();
        developerSettingsModel = applicationComponent.developerSettingsModel();
        newYearModel = applicationComponent.newYearModel();
        pinCodeModel = applicationComponent.pinCodeModel();
        pinState = applicationComponent.pinState();
        nameAlternativesModel = applicationComponent.nameAlternativesModel();
        simpleStorage = applicationComponent.simpleStorage();
        widgetsModel = applicationComponent.mailWidgetsModel();
        channelSynchronizer = applicationComponent.channelSynchronizer();
        mailMessagingService = applicationComponent.mailMessagingService();
        crossAccountModel = applicationComponent.crossAccountModel();
        accountComponentProvider = applicationComponent.accountComponentProvider();
        dataManagingExecutor = (TestDataManagingExecutor) applicationComponent.dataManagingExecutor();
    }

    public void initAccountModels(@NonNull AccountComponent accountComponent, boolean enableTabs) {
        accountSettings = accountComponent.settings();
        accountSettings.edit().setTabsEnabled(enableTabs).setSyncedWithServer().commitAndSync();
        messagesModel = accountComponent.messagesModel();
        foldersModel = accountComponent.foldersModel();
        threadsModel = accountComponent.threadsModel();
        labelsModel = accountComponent.labelsModel();
        searchModel = accountComponent.searchModel();
        searchSuggestsModel = accountComponent.searchSuggestsModel();
        attachmentsModel = accountComponent.attachmentsModel();
        draftAttachmentsModel = accountComponent.draftAttachmentsModel();
        syncModel = accountComponent.syncModel();
        transacter = accountComponent.transacter();
        ftsSQLite = accountComponent.ftsStorIOSQLite();
        abookModel = accountComponent.abookModel();
        draftsModel = accountComponent.draftsModel();
        mailApi = accountComponent.api();
        nanoSettingsModel = accountComponent.settingsModel();
        gson = accountComponent.gson();
        composeStoreModel = accountComponent.composeStoreModel();
        cleanupModel = accountComponent.cleanupModel();
        translatorModel = accountComponent.translatorModel();
        addressModel = accountComponent.addressModel();
        newsLettersModel = accountComponent.newsLetterModel();
        unsubscribeTipStrategy = accountComponent.unsubscribeTipStrategy();
        attachmentPreviewModel = accountComponent.attachmentPreviewModel();
    }
}
