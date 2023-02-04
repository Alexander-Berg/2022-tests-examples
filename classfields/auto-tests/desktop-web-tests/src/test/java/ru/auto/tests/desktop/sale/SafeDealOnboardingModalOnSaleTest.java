package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SAFE_DEAL;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.FORCE_POPUP;
import static ru.auto.tests.desktop.consts.QueryParams.SAFE_DEAL_SELLER_ONBOARDING_MODAL;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.step.CookieSteps.SAFE_DEAL_SELLER_ONBOARDING_PROMO;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попапы «Безопасная сделка» на карточке")
@Feature(SALES)
@Story(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SafeDealOnboardingModalOnSaleTest {

    private static final String SALE_ID = getRandomOfferId();

    private static final String SAFE_DEAL_POPUP = "Безопасная сделка\nМы пересмотрели сделку купли-продажи " +
            "автомобиля, разложили её по полочкам и перенесли в онлайн для вашего удобства и безопасности\n" +
            "Удобно\nСделка разделена на понятные шаги, документы заполняются онлайн. Остаётся только распечатать " +
            "готовый договор и подписать\nБезопасно\nДоговор купли-продажи составлен юристами Авто.ру. Автомобиль и " +
            "собственник проверяются по базам. Перевод денег — онлайн\nПодробнее\nПонятно";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(mockOffer(CAR_EXAMPLE).setId(SALE_ID).getResponse()),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH)
                .addParam(FORCE_POPUP, SAFE_DEAL_SELLER_ONBOARDING_MODAL).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа «Безопасная сделка» на карточке + установка куки")
    public void shouldSeeSafeDealPopupText() {
        basePageSteps.onCardPage().safeDealOnboardingPopup().waitUntil(isDisplayed()).should(hasText(SAFE_DEAL_POPUP));
        cookieSteps.shouldSeeCookieWithValue(SAFE_DEAL_SELLER_ONBOARDING_PROMO, "-1");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем попап «Безопасная сделка» на карточке по крестику")
    public void shouldCloseSafeDealPopup() {
        basePageSteps.onCardPage().safeDealOnboardingPopup().closeIcon().waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().safeDealOnboardingPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрываем попап «Безопасная сделка» на карточке по кнопке «Понятно»")
    public void shouldCloseSafeDealPopupByUnderstandButton() {
        basePageSteps.onCardPage().safeDealOnboardingPopup().button("Понятно").waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().safeDealOnboardingPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Жмем «Подробнее» в попапе «Безопасная сделка» на карточке")
    public void shouldClickDetailedSafeDealPopup() {
        basePageSteps.onCardPage().safeDealOnboardingPopup().button("Подробнее").waitUntil(isDisplayed()).click();

        urlSteps.testing().path(PROMO).path(SAFE_DEAL).shouldNotSeeDiff();
    }

}
