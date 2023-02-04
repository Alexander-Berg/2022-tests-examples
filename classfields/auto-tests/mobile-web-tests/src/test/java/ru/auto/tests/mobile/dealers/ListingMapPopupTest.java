package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.DEALER_ID;
import static ru.auto.tests.desktop.consts.QueryParams.DEALER_LISTING_MAP;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Карта дилеров - поп-ап дилера")
@Feature(AutoruFeatures.DEALERS)
public class ListingMapPopupTest {

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("mobile/AutoruDealerDealerId"),
                stub("desktop/SalonPhones")
        ).create();

        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(ALL)
                .addParam(DEALER_ID, "20699478").open();

        clickMapPoint();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeMapPopup() {
        basePageSteps.onDealerMapPage().mapPopup().waitUntil(isDisplayed()).should(hasText("Авилон Mercedes-Benz " +
                "Воздвиженка\nОфициальный дилер Mercedes-Benz\nСеть АВИЛОН\n433 предложения \n" +
                "Москва, ул. Воздвиженка, д. 12\nПоказать телефон"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать телефон»")
    public void shouldClickShowPhoneButton() {
        basePageSteps.onDealerMapPage().mapPopup().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onDealerMapPage().popup().waitUntil(isDisplayed()).should(hasText("Телефон\n+7 495 266-44-41\n" +
                "с 09:00 до 22:00\n+7 495 266-44-42\nс 10:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по поп-апу на карте")
    public void shouldClickMapPopup() {
        mockRule.setStubs(
                stub("desktop/Salon")
        ).update();

        basePageSteps.onDealerMapPage().mapPopup().click();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).path(SLASH)
                .addParam(FROM, DEALER_LISTING_MAP).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Авто в продаже»")
    public void shouldClickSalesUrl() {
        mockRule.setStubs(
                stub("desktop/Salon")
        ).update();

        basePageSteps.onDealerMapPage().mapPopup().salesUrl().should(isDisplayed()).click();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).path(SLASH)
                .addParam(FROM, DEALER_LISTING_MAP).shouldNotSeeDiff();
    }

    @Step("Кликаем по иконке карты")
    public void clickMapPoint() {
        basePageSteps.onDealersListingPage().mapIcon().should(isDisplayed()).click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.moveCursorAndClick(basePageSteps.onDealerMapPage().mapPoint());
        basePageSteps.onDealerMapPage().mapPopup().waitUntil(isDisplayed());
    }

}
