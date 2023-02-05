package com.yandex.mail.main;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.R;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.AccountComponentProvider;
import com.yandex.mail.di.ApplicationComponent;
import com.yandex.mail.entity.AccountEntity;
import com.yandex.mail.entity.FolderType;
import com.yandex.mail.filters.FilterClass;
import com.yandex.mail.filters.FilterRule;
import com.yandex.mail.filters.promo.FilterPromoChainModel;
import com.yandex.mail.message_container.FolderContainer;
import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.FoldersModel;
import com.yandex.mail.model.GeneralSettingsModel;
import com.yandex.mail.model.NewsLettersModel;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.settings.SimpleStorage;
import com.yandex.mail.storage.entities.EntitiesTestFactory;
import com.yandex.mail.theme.NoTheme;
import com.yandex.mail.theme.Theme;
import com.yandex.mail.theme.ThemeModel;
import com.yandex.mail.util.ActionTimeTracker;
import com.yandex.mail.util.SalesInAppController;
import com.yandex.mail.util.TestBasePresenterConfig;
import com.yandex.mail360.offline_service.shtorka.PhotoMemoriesModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;

import static com.yandex.mail.storage.entities.EntitiesTestFactory.buildAccountEntity;
import static com.yandex.mail.util.ActionTimeTracker.Actions.REPORT_GOOGLE_PLAY_SERVICES_VERSION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(IntegrationTestRunner.class)
public class MailActivityPresenterTest {

    @Mock
    @NonNull
    BaseMailApplication mockApplication;

    @Mock
    @NonNull
    MailActivityView mockView;

    @Mock
    @NonNull
    ThemeModel mockThemeModel;

    @Mock
    @NonNull
    SimpleStorage simpleStorage;

    @Mock
    @NonNull
    YandexMailMetrica yandexMailMetrica;

    @Mock
    @NonNull
    AccountModel accountModel;

    @Mock
    @NonNull
    GeneralSettingsModel settingsModel;

    @Mock
    @NonNull
    ActionTimeTracker actionTimeTracker;

    @Mock
    @NonNull
    NewsLettersModel newsLettersModel;

    @NonNull
    private MailActivityPresenter presenter;

    @Mock // @Before
    ApplicationComponent applicationComponent;

    @Mock
    @NonNull
    AccountComponent mockAccountComponent;

    @Mock
    @NonNull
    FilterPromoChainModel filterPromoChainModel;

    @Mock
    @NonNull
    AccountComponentProvider mockAccountComponentProvider;

    @Mock
    @NonNull
    FoldersModel foldersModel;

    @Before
    public void beforeEachTest() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        AccountEntity account = mock(AccountEntity.class);
        when(account.getUid()).thenReturn(1L);
        when(accountModel.observeSelectedAccount()).thenReturn(Flowable.just(Optional.of(account)));
        when(mockThemeModel.observeThemesForAccount(anyLong())).thenReturn(Observable.just(NoTheme.INSTANCE));
        mockPackageManager();
        when(settingsModel.observeThreadMode(1L)).thenReturn(Observable.just(true));

        when(simpleStorage.getBoolean(anyString(), anyBoolean())).thenReturn(false);

        when(mockAccountComponent.isYaTeam()).thenReturn(false);
        when(mockAccountComponent.foldersModel()).thenReturn(foldersModel);
        when(mockAccountComponent.salesInAppController()).thenReturn(mock(SalesInAppController.class));
        when(mockAccountComponent.photoMemoriesModel()).thenReturn(mock(PhotoMemoriesModel.class));
        when(foldersModel.getDefaultFolder()).thenReturn(Single.just(Optional.of(EntitiesTestFactory.buildNanoFolder())));
        when(mockApplication.getAccountComponent(anyLong())).thenReturn(mockAccountComponent);
        when(mockApplication.getApplicationContext()).thenReturn(mockApplication);
        when(mockApplication.getApplicationComponent()).thenReturn(applicationComponent);
        when(applicationComponent.simpleStorage()).thenReturn(simpleStorage);
        when(filterPromoChainModel.getPromoFlowable()).thenReturn(Flowable.empty());

        presenter = new MailActivityPresenter(
                mockApplication,
                mockThemeModel,
                actionTimeTracker,
                yandexMailMetrica,
                new TestBasePresenterConfig().getConfig(),
                accountModel,
                settingsModel,
                filterPromoChainModel,
                mockAccountComponentProvider
        );
    }

    @Test
    public void setUid_handlesNoUid() {
        final long uid = 1L;
        when(mockThemeModel.observeThemesForAccount(uid)).thenReturn(Observable.just(NoTheme.INSTANCE));
        presenter.onBindView(mockView);
        presenter.setUid(uid);

        presenter.setUid(Constants.NO_UID);
        // should not throw exceptions
    }

    @Test
    public void setUid_handlesNullSettings() {
        final long uid = 1L;
        when(mockThemeModel.observeThemesForAccount(uid)).thenReturn(Observable.just(NoTheme.INSTANCE));
        presenter.onBindView(mockView);
        presenter.setUid(uid);
    }

    @Test
    public void bindView_shouldSendGSMVersionToMetrica() {
        when(actionTimeTracker.happenedAgo(1, DAYS, REPORT_GOOGLE_PLAY_SERVICES_VERSION)).thenReturn(true);
        presenter.onBindView(mockView);
        verify(yandexMailMetrica).reportEvent(mockApplication.getString(R.string.metrica_google_play_services_version), singletonMap("version", 1));
    }

    @Test
    public void bindView_shouldNotSendGSMVersionToMetricaIfAlreadySent() {
        when(actionTimeTracker.happenedAgo(1, DAYS, REPORT_GOOGLE_PLAY_SERVICES_VERSION)).thenReturn(false);
        presenter.onBindView(mockView);
        verify(yandexMailMetrica, never()).reportEvent(
                mockApplication.getString(R.string.metrica_google_play_services_version),
                singletonMap("version", 1)
        );
    }

    @Test
    public void bindView_shouldObserveSelectedAccount() {
        PublishProcessor<Optional<AccountEntity>> accountSubject = PublishProcessor.create();
        when(accountModel.observeSelectedAccount()).thenReturn(accountSubject);

        presenter = new MailActivityPresenter(
                mockApplication,
                mockThemeModel,
                actionTimeTracker,
                yandexMailMetrica,
                new TestBasePresenterConfig().getConfig(),
                accountModel,
                settingsModel,
                filterPromoChainModel,
                mockAccountComponentProvider
        );
        presenter.onBindView(mockView);

        AccountEntity account1 = buildAccountEntity(1L, true);
        when(settingsModel.observeThreadMode(account1.getUid())).thenReturn(Observable.just(true));
        accountSubject.onNext(Optional.of(account1));
        verify(mockView).onAccountChanged(account1.getUid());

        AccountEntity account2 = buildAccountEntity(2L, true);
        when(settingsModel.observeThreadMode(account2.getUid())).thenReturn(Observable.just(true));
        accountSubject.onNext(Optional.of(account2));
        verify(mockView).onAccountChanged(account2.getUid());
    }

    @Test
    public void setUid_shouldResubscribeForTheme() {
        PublishProcessor<Optional<AccountEntity>> accountProcessor = PublishProcessor.create();
        when(accountModel.observeSelectedAccount()).thenReturn(accountProcessor);

        presenter = new MailActivityPresenter(
                mockApplication,
                mockThemeModel,
                actionTimeTracker,
                yandexMailMetrica,
                new TestBasePresenterConfig().getConfig(),
                accountModel,
                settingsModel,
                filterPromoChainModel,
                mockAccountComponentProvider
        );
        presenter.onBindView(mockView);

        AccountEntity account1 = buildAccountEntity(1L, true);
        Theme theme1 = mock(Theme.class);
        when(mockThemeModel.observeThemesForAccount(account1.getUid())).thenReturn(Observable.just(theme1));
        when(settingsModel.observeThreadMode(account1.getUid())).thenReturn(Observable.just(true));
        accountProcessor.onNext(Optional.of(account1));
        verify(mockView).onThemeUpdated(theme1);

        AccountEntity account2 = buildAccountEntity(2L, true);
        Theme theme2 = mock(Theme.class);
        when(mockThemeModel.observeThemesForAccount(account2.getUid())).thenReturn(Observable.just(theme2));
        when(settingsModel.observeThreadMode(account2.getUid())).thenReturn(Observable.just(true));
        accountProcessor.onNext(Optional.of(account2));
        verify(mockView).onThemeUpdated(theme2);
    }

    @Test
    public void setUid_shouldObserveThreadMode() {
        PublishSubject<Boolean> threadModeSubject = PublishSubject.create();
        when(settingsModel.observeThreadMode(1L)).thenReturn(threadModeSubject);

        presenter.onBindView(mockView);
        presenter.setUid(1L);

        threadModeSubject.onNext(true);
        verify(mockView).onThreadModeChanged(true);

        threadModeSubject.onNext(false);
        verify(mockView).onThreadModeChanged(false);
    }

    @Test
    public void setPendingUidAndFolder_shouldChangeAccount() {
        presenter.onBindView(mockView);
        presenter.setUid(1L);

        presenter.setPendingUidAndFolder(2L, new FolderContainer(10, FolderType.USER.getServerType()));
        verify(accountModel).selectAccount(2L);
    }

    @Test
    public void showPromo_whenPromoEmits() {
        when(filterPromoChainModel.getPromoFlowable()).thenReturn(
                Flowable.just(new FilterPromoChainModel.PromoInfo(
                        1L,
                        "",
                        "",
                        new FilterRule(1, true, emptyList(), emptyList(), FilterClass.MIXED, 1),
                        ""
                ))
        );

        presenter.onBindView(mockView);
        presenter.setUid(1L);
        presenter.onResumeHook();

        verify(mockView).showFilterPromo(any());
    }

    private void mockPackageManager() throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = 1;
        when(packageManager.getPackageInfo("com.google.android.gms", 0)).thenReturn(packageInfo);
    }
}
