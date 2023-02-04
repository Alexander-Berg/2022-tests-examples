package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER_ID;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALER_NET;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.COOKIESYNC;
import static ru.auto.tests.desktop.consts.QueryParams.DEALER_ID;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.GEO_ID;
import static ru.auto.tests.desktop.consts.QueryParams.HAS_OFFER;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Поп-ап на карте")
@Feature(DEALERS)
@Story(DEALER_CARD)
public class ListingMapPopupTest {

    private String dealerUrl;

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
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/AutoruBreadcrumbsNewUsed"),
                stub("desktop/SessionUnauth"),
                stub("desktop/AutoruDealerDealerId"),
                stub("desktop/AutoruDealerDealerIdLatitude"),
                stub("desktop/Salon"),
                stub("desktop/SalonPhones")
        ).create();

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);

        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL)
                .addParam(DEALER_ID, CARS_OFFICIAL_DEALER_ID).open();

        dealerUrl = urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).path(SLASH).addParam(FROM, "dealer-listing-map").toString();
        clickMapPoint();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeMapPopup() {
        basePageSteps.onDealerListingPage().mapPopup().should(isDisplayed())
                .should(hasText("Проверенный дилер\nАвилон Mercedes-Benz Воздвиженка\nОфициальный дилер\nСеть АВИЛОН\n" +
                        "Москва, ул. Воздвиженка, д.12\nм. Арбатская (Филевская), Арбатская (Арбатско-Покровская)\n" +
                        "453 авто в продаже\nПоказать телефон +7 XXX XXX-XX-XX"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Телефоны в поп-апе")
    public void shouldSeePhones() {
        basePageSteps.onDealerListingPage().mapPopup().showPhones().waitUntil(isDisplayed()).click();

        basePageSteps.onDealerListingPage().mapPopup().showPhones().should(isDisplayed())
                .should(hasText("+7 495 266-44-41, +7 495 266-44-42"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по поп-апу на карте")
    public void shouldClickMapPopup() {
        basePageSteps.onDealerListingPage().mapPopup().waitUntil(isDisplayed()).click();

        urlSteps.ignoreParam(HAS_OFFER)
                .ignoreParam(COOKIESYNC)
                .shouldNotDiffWith(dealerUrl);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию дилера")
    public void shouldClickDealerName() {
        basePageSteps.onDealerListingPage().mapPopup().name().waitUntil(isDisplayed()).click();

        urlSteps.ignoreParam(HAS_OFFER)
                .ignoreParam(COOKIESYNC)
                .shouldNotDiffWith(dealerUrl);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию сети")
    public void shouldClickDealerNetName() {
        mockRule.setStubs(stub("desktop/AutoruBreadcrumbsStateNew"),
                stub("desktop/AutoruDealerAvilon"),
                stub("desktop/DealerNetAvilon")).update();

        basePageSteps.onDealerListingPage().mapPopup().netUrl().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(DEALER_NET).path("/avilon/")
                .ignoreParam(HAS_OFFER)
                .ignoreParam(GEO_ID)
                .shouldNotSeeDiff();

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Авто в продаже»")
    public void shouldClickSalesUrl() {
        basePageSteps.onDealerListingPage().mapPopup().salesUrl().waitUntil(isDisplayed()).click();

        urlSteps.onCurrentUrl().ignoreParam(HAS_OFFER).shouldNotDiffWith(dealerUrl);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие поп-апа")
    public void shouldClosePopup() {
        basePageSteps.onDealerListingPage().mapPopup().close().waitUntil(isDisplayed(), 5).click();

        basePageSteps.onDealerListingPage().mapPopup().should(not(isDisplayed()));
    }

    @Step("Кликаем по точке на карте")
    public void clickMapPoint() {
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.moveCursorAndClick(basePageSteps.onDealerListingPage().mapPoint().waitUntil(isDisplayed()));
        basePageSteps.onDealerListingPage().mapPopup().waitUntil(isDisplayed());
    }
}