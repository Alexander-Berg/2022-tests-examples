package ru.yandex.market.fragment.main.profile;

import com.annimon.stream.OptionalBoolean;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;

import java.util.Collections;

import ru.yandex.market.BaseTest;
import ru.yandex.market.analitycs.health.MetricaSender;
import ru.yandex.market.analytics.facades.NavigationAnalyticsFacade;
import ru.yandex.market.analytics.facades.ProfileMenuAnalytics;
import ru.yandex.market.analytics.facades.SkillGroupChatAnalytics;
import ru.yandex.market.analytics.facades.SmartCoinsEntryPointExpAnalytics;
import ru.yandex.market.analytics.facades.VacanciesAnalytics;
import ru.yandex.market.analytics.facades.health.LoginHealthFacade;
import ru.yandex.market.analytics.facades.health.SupportChatHealthFacade;
import ru.yandex.market.analytics.health.HealthErrorFormatter;
import ru.yandex.market.analytics.health.HealthService;
import ru.yandex.market.clean.domain.model.SupportChannel;
import ru.yandex.market.clean.domain.model.profile.ProfileAdditionalInfo;
import ru.yandex.market.clean.presentation.feature.cms.item.WidgetDataFlow;
import ru.yandex.market.clean.presentation.feature.profile.ProfilePresenter;
import ru.yandex.market.clean.presentation.feature.profile.ProfileUseCases;
import ru.yandex.market.clean.presentation.feature.profile.ProfileView;
import ru.yandex.market.clean.presentation.feature.profile.UserProfileFormatter;
import ru.yandex.market.clean.presentation.feature.profile.UserProfileVo;
import ru.yandex.market.clean.presentation.feature.profile.UserProfileVoTestFactory;
import ru.yandex.market.clean.presentation.feature.profile.menu.ProfileMenuItemFormatter;
import ru.yandex.market.clean.presentation.feature.profile.promo.ProfilePromoItemFormatter;
import ru.yandex.market.clean.presentation.navigation.Router;
import ru.yandex.market.common.android.ResourcesManager;
import ru.yandex.market.common.featureconfigs.managers.OrderConsultationToggleManager;
import ru.yandex.market.common.featureconfigs.models.FeatureToggle;
import ru.yandex.market.common.featureconfigs.models.SupportChatterboxConfig;
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider;
import ru.yandex.market.domain.user.model.UserProfile;
import ru.yandex.market.domain.user.model.UserProfileTestFactory;
import ru.yandex.market.optional.Optional;
import ru.yandex.market.rx.schedulers.YSchedulers;
import ru.yandex.market.util.help.HelpLink;
import ru.yandex.market.web.MarketWebUrlProviderFactory;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.TestExtensionsKt.presentationSchedulersMock;

public class ProfilePresenterTest extends BaseTest {

    private static final long USER_INFO_ID = 432;
    private static final String SUPPORT_CHAT_LINK =
            "https://help-frontend.taxi.yandex.ru/ecom/yandex/yp/ru_ru/market/chat";

    private static final UserProfile DUMMY_USER_PROFILE = UserProfileTestFactory.create(USER_INFO_ID);

    @Mock
    private ProfileUseCases useCases;

    @Mock
    private HelpLink helpLink;

    @Mock
    private FeatureConfigsProvider featureConfigsProvider;

    @Mock
    private SkillGroupChatAnalytics skillGroupChatAnalytics;

    @Mock
    private OrderConsultationToggleManager orderConsultationToggleManager;

    @Mock
    private AuthStateController authStateController;

    @Mock
    private Router router;

    @Mock
    private ProfileView profileView;

    @Mock
    @SuppressWarnings("deprecation")
    private ru.yandex.market.analitycs.AnalyticsService analyticsService;

    @Mock
    private NavigationAnalyticsFacade navigationAnalyticsFacade;

    @Mock
    private MetricaSender metricaSender;

    @Mock
    private ResourcesManager resourcesManager;

    @Mock
    private UserProfileFormatter viewObjectMapper;

    @Mock
    private ProfileMenuItemFormatter profileMenuItemFormatter;

    @Mock
    private ProfilePromoItemFormatter profilePromoItemFormatter;

    @Mock
    private SmartCoinsEntryPointExpAnalytics smartCoinsAnalytics;

    @Mock
    private ProfileMenuAnalytics profileMenuAnalytics;

    @Mock
    private VacanciesAnalytics vacanciesAnalytics;

    @Mock
    private MarketWebUrlProviderFactory marketWebUrlProviderFactory;

    @Mock
    private SupportChatHealthFacade supportChatHealthFacade;

    @Mock
    private Lazy<LoginHealthFacade> loginHealthFacade;

    @Mock
    private HealthService healthService;

    @Mock
    private HealthErrorFormatter healthErrorFormatter;

    @Mock
    private Lazy<WidgetDataFlow> widgetDataFlow;

    private ProfilePresenter presenter;

    private final PublishSubject<AuthStateController.State> publisher = PublishSubject.create();

    @BeforeClass
    public static void setup() {
        YSchedulers.setTestMode();
    }

    @AfterClass
    public static void tearDown() {
        YSchedulers.setProdMode();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(useCases.getCurrentUserProfile()).thenReturn(Observable.just(Optional.Companion.of(DUMMY_USER_PROFILE)));
        when(useCases.getMenuItemTypes(any(), anyBoolean())).thenReturn(Observable.just(Collections.emptyList()));
        when(useCases.getPromoItemTypes(any())).thenReturn(Single.just(Collections.emptyList()));
        when(useCases.setShouldShowUserPublicationsBadge(false)).thenReturn(Completable.complete());
        when(useCases.getUserPublicationsHintShown()).thenReturn(Observable.just(OptionalBoolean.of(true)));
        when(useCases.getProfileAdditionalInfo(any(), any()))
                .thenReturn(Observable.just(ProfileAdditionalInfo.getEMPTY()));
        when(useCases.supportChannels()).thenReturn(Single.just(
                Collections.singletonList(new SupportChannel.Chat(SUPPORT_CHAT_LINK))
        ));
        when(useCases.getChatterboxConfigUseCase()).thenReturn(Single.just(
                new SupportChatterboxConfig.SupportChatterboxConfigEnable(SUPPORT_CHAT_LINK)
        ));

        when(viewObjectMapper.format(DUMMY_USER_PROFILE)).thenReturn(UserProfileVoTestFactory.create());

        doReturn(publisher).when(authStateController).getAuthStateChangeObservable();
        doAnswer(invocation -> {
            changeAuthState(AuthStateController.State.LOGGED_IN);
            return null;
        }).when(profileView).requestAuth();
        doAnswer(invocation -> {
            changeAuthState(AuthStateController.State.LOGGED_OUT);
            return null;
        }).when(authStateController).logout();
        presenter = new ProfilePresenter(
                presentationSchedulersMock(),
                useCases,
                authStateController,
                router,
                viewObjectMapper,
                analyticsService,
                navigationAnalyticsFacade,
                metricaSender,
                resourcesManager,
                profileMenuItemFormatter,
                profilePromoItemFormatter,
                smartCoinsAnalytics,
                profileMenuAnalytics,
                vacanciesAnalytics,
                marketWebUrlProviderFactory,
                skillGroupChatAnalytics,
                helpLink,
                supportChatHealthFacade,
                loginHealthFacade,
                widgetDataFlow
        );


        when(featureConfigsProvider.getOrderConsultationToggleManager()).thenReturn(orderConsultationToggleManager);
        when(orderConsultationToggleManager.get()).thenReturn(new FeatureToggle(false));
    }

    @Test
    public void testNotLoggedState() {
        changeAuthState(AuthStateController.State.LOGGED_OUT);
        presenter.attachView(profileView);

        InOrder order = inOrder(profileView);
        order.verify(profileView).showLoginProgress();
        order.verify(profileView).showProfile(notNull());
        order.verify(profileView, new Times(2)).showMenuItems(any());
        order.verify(profileView).showPromoItems(any());
    }

    @Test
    public void testLoggedInState() {
        changeAuthState(AuthStateController.State.LOGGED_IN);
        presenter.attachView(profileView);

        InOrder order = inOrder(profileView);
        order.verify(profileView).showLoginProgress();
        order.verify(profileView).showProfile(notNull(UserProfileVo.class));
        order.verify(profileView, new Times(2)).showMenuItems(any());
        order.verify(profileView).showPromoItems(any());
    }

    @Test
    public void testLogin() {
        changeAuthState(AuthStateController.State.LOGGED_OUT);
        presenter.attachView(profileView);

        presenter.login();

        InOrder order = inOrder(profileView);
        // initial
        order.verify(profileView).showLoginProgress();
        // after change
        order.verify(profileView).requestAuth();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testLogout() {
        when(widgetDataFlow.get()).thenReturn(new WidgetDataFlow());
        when(useCases.logout()).thenReturn(Completable.complete());
        when(useCases.clearPaymentOptions()).thenReturn(Completable.complete());
        when(useCases.setCheckoutCheckboxFirstAppearance(null)).thenReturn(Completable.complete());
        when(useCases.clearUserICookie()).thenReturn(Completable.complete());
        when(useCases.clearDailyBonusDataStore()).thenReturn(Completable.complete());
        when(useCases.resetUpsellVisited()).thenReturn(Completable.complete());
        when(loginHealthFacade.get()).thenReturn(new LoginHealthFacade(healthService, healthErrorFormatter));

        changeAuthState(AuthStateController.State.LOGGED_IN);
        presenter.attachView(profileView);
        presenter.logout();

        verify(profileView, new Times(2)).showProfile(any());
        verify(profileView, new Times(4)).showMenuItems(any());
        verify(profileView, new Times(2)).showPromoItems(any());
        verify(useCases, new Times(1)).logout();
        verify(useCases, new Times(1)).clearPaymentOptions();
    }

    private void changeAuthState(AuthStateController.State state) {
        doReturn(Single.just(state)).when(authStateController).getCurrentState();
        publisher.onNext(state);
    }
}
