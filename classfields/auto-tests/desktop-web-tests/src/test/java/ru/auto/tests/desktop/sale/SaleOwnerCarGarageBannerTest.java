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
import ru.auto.tests.desktop.mock.MockGarageCard;
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
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.card.AddedToGarageBanner.TO_GARAGE;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockOffer.ownerCar;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD_OFFER;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.step.CookieSteps.GARAGE_BANNER_CLOSED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннер «Мы добавили ваш автомобиль в Гараж» на карточке")
@Feature(SALES)
@Story("Баннер «Мы добавили ваш автомобиль в Гараж»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SaleOwnerCarGarageBannerTest {

    private static final String SALE_ID = "1076842087-f1e84";
    private static final String GARAGE_CARD_ID = "790117861";
    private static final String REDIRECTED = "redirected";

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
                sessionAuthUserStub(),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(ownerCar().setAcceptableForGarage(true).getResponse())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок «Мы добавили ваш автомобиль в Гараж» с кукой «garage_banner_closed = redirected»")
    public void shouldSeeNoAddedToGarageBlockWithGarageBannerClosedCookie() {
        cookieSteps.setCookieForBaseDomain(GARAGE_BANNER_CLOSED, REDIRECTED);
        urlSteps.refresh();

        basePageSteps.onCardPage().addedToGarageBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Устанавливается кука «garage_banner_closed = redirected» при переходе в гараж с карточки")
    public void shouldSeeGarageBannerClosedCookieRedirected() {
        setGarageMocks();

        basePageSteps.onCardPage().addedToGarageBanner().button(TO_GARAGE).click();

        cookieSteps.shouldSeeCookieWithValue(GARAGE_BANNER_CLOSED, REDIRECTED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Устанавливается кука «garage_banner_closed = closed» при закрытии «Мы добавили ваш автомобиль в Гараж»")
    public void shouldSeeGarageBannerClosedCookieClosed() {
        setGarageMocks();

        basePageSteps.onCardPage().addedToGarageBanner().close().waitUntil(isDisplayed()).click();

        cookieSteps.shouldSeeCookieWithValue(GARAGE_BANNER_CLOSED, "closed");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход в гараж с карточки из блока «Мы добавили ваш автомобиль в Гараж»")
    public void shouldGoToGarage() {
        setGarageMocks();

        basePageSteps.onCardPage().addedToGarageBanner().button(TO_GARAGE).click();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается блок «Мы добавили ваш автомобиль в Гараж» с «acceptable_for_garage = false»")
    public void shouldSeeNoAddedToGarageBlockWithAcceptableForGarageFalse() {
        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(ownerCar().setAcceptableForGarage(false).getResponse()));

        urlSteps.refresh();

        basePageSteps.onCardPage().addedToGarageBanner().should(not(isDisplayed()));
    }

    private void setGarageMocks() {
        MockGarageCard garageCard = garageCardOffer().setId(GARAGE_CARD_ID);

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s", GARAGE_USER_CARD_OFFER, SALE_ID))
                        .withResponseBody(garageCard.getBody()),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCard.getBody())
        ).update();
    }

}
