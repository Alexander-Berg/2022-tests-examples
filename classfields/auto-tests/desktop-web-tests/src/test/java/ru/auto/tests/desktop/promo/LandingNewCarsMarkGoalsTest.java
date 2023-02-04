package ru.auto.tests.desktop.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LANDING_NEW_CARS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Goals.CARD_VIEW_LANDING_NEWAUTO;
import static ru.auto.tests.desktop.consts.Goals.PHONE_LANDING_NEWAUTO_ALL;
import static ru.auto.tests.desktop.consts.Goals.PHONE_LANDING_NEWAUTO_POPUP;
import static ru.auto.tests.desktop.consts.Goals.PHONE_LANDING_SHAPKA;
import static ru.auto.tests.desktop.consts.Goals.PHONE_TYPING_LANDING_SHAPKA;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.KIA;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.DEBUG;
import static ru.auto.tests.desktop.consts.QueryParams.TRUE;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.noRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.page.PromoCarsLandingPage.GET_BEST_PRICE;
import static ru.auto.tests.desktop.page.PromoCarsLandingPage.PHONE_NUMBER;
import static ru.auto.tests.desktop.step.CookieSteps.PROMO_NEW_CARS_FIRST_LETTER_TYPED;

@DisplayName("Цели на лэндинге новых авто марки")
@Epic(PROMO)
@Feature(LANDING_NEW_CARS)
@Story("Страница марки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class LandingNewCarsMarkGoalsTest {

    private static final String PHONE = "79111234567";
    private static final String SHORT_PHONE = "79";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(KIA).path(Pages.PROMO).addParam(DEBUG, TRUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цель «PHONE_TYPING_LANDING_SHAPKA» при старте ввода телефона в шапке")
    public void shouldSeePhoneTypingLandingShapka() {
        basePageSteps.onPromoCarsLandingPage().banner().input(PHONE_NUMBER).sendKeys(SHORT_PHONE);

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(PHONE_TYPING_LANDING_SHAPKA))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сеттится кука «promo_new_cars_first_letter_typed_cookie» при старте ввода телефона в шапке")
    public void shouldSeeCookie() {
        basePageSteps.onPromoCarsLandingPage().banner().input(PHONE_NUMBER).sendKeys(SHORT_PHONE);

        cookieSteps.shouldSeeCookie(PROMO_NEW_CARS_FIRST_LETTER_TYPED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Нет цели «PHONE_TYPING_LANDING_SHAPKA» при вводе телефона с кукой «promo_new_cars_first_letter_typed_cookie»")
    public void shouldSeeNoPhoneTypingLandingShapkaWithCookie() {
        cookieSteps.setCookieForBaseDomain(PROMO_NEW_CARS_FIRST_LETTER_TYPED, TRUE);
        basePageSteps.refresh();
        basePageSteps.onPromoCarsLandingPage().banner().input(PHONE_NUMBER).sendKeys(SHORT_PHONE);

        seleniumMockSteps.assertWithWaiting(noRequest(
                hasGoal(seleniumMockSteps.formatGoal(PHONE_TYPING_LANDING_SHAPKA))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цель «PHONE_LANDING_SHAPKA» по клику на «Получить лучшую цену» в шапке")
    public void shouldSeePhoneLandingShapka() {
        basePageSteps.onPromoCarsLandingPage().banner().input(PHONE_NUMBER).sendKeys(PHONE);
        basePageSteps.onPromoCarsLandingPage().banner().button(GET_BEST_PRICE).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(PHONE_LANDING_SHAPKA))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цель «PHONE_LANDING_NEWAUTO_ALL» по клику на «Получить лучшую цену» в шапке")
    public void shouldSeePhoneLandingNewautoAll() {
        basePageSteps.onPromoCarsLandingPage().banner().input(PHONE_NUMBER).sendKeys(PHONE);
        basePageSteps.onPromoCarsLandingPage().banner().button(GET_BEST_PRICE).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(PHONE_LANDING_NEWAUTO_ALL))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цель «CARD_VIEW_LANDING_NEWAUTO» по клику на снипет")
    public void shouldSeeCardViewLandingNewauto() {
        basePageSteps.onPromoCarsLandingPage().snippet().get(0).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(CARD_VIEW_LANDING_NEWAUTO))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цель «PHONE_LANDING_NEWAUTO_POPUP» по клику на «Получить лучшую цену» в попапе")
    public void shouldSeePhoneLandingNewautoPopup() {
        basePageSteps.onPromoCarsLandingPage().snippet().get(0).click();
        basePageSteps.onPromoCarsLandingPage().popup().input(PHONE_NUMBER).sendKeys(PHONE);
        basePageSteps.onPromoCarsLandingPage().popup().button(GET_BEST_PRICE).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(PHONE_LANDING_NEWAUTO_POPUP))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цель «PHONE_LANDING_NEWAUTO_ALL» по клику на «Получить лучшую цену» в попапе")
    public void shouldSeePhoneLandingNewautoAllFromPopup() {
        basePageSteps.onPromoCarsLandingPage().snippet().get(0).click();
        basePageSteps.onPromoCarsLandingPage().popup().input(PHONE_NUMBER).sendKeys(PHONE);
        basePageSteps.onPromoCarsLandingPage().popup().button(GET_BEST_PRICE).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(PHONE_LANDING_NEWAUTO_ALL))
        ));
    }

}
