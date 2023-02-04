package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

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
import static ru.auto.tests.desktop.consts.QueryParams.DEALER_ID;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Сниппет")
@Feature(DEALERS)
@Story(DEALER_CARD)
public class ListingSnippetTest {

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
                stub("desktop/AutoruBreadcrumbsNewUsed"),
                stub("desktop/SessionUnauth"),
                stub("desktop/AutoruDealerDealerId"),
                stub("desktop/Salon"),
                stub("desktop/SalonPhones")
        ).create();

        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL)
                .addParam(DEALER_ID, CARS_OFFICIAL_DEALER_ID).open();

        dealerUrl = urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).path(SLASH).addParam("from", "dealer-listing-list").toString();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeDealer() {
        basePageSteps.onDealerListingPage().getDealer(0).should(hasText("Проверенный дилер\n" +
                "Авилон Mercedes-Benz Воздвиженка\nОфициальный дилер\nСеть АВИЛОН\nМосква, ул. Воздвиженка, д.12\n" +
                "м. Арбатская (Филевская), Арбатская (Арбатско-Покровская)\n453 авто в продаже\n" +
                "Показать телефон +7 XXX XXX-XX-XX"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Телефон в сниппете")
    public void shouldSeePhones() {
        basePageSteps.onDealerListingPage().getDealer(0).showPhones().waitUntil(isDisplayed()).click();

        basePageSteps.onDealerListingPage().getDealer(0).showPhones()
                .should(hasText("+7 495 266-44-41, +7 495 266-44-42"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по сниппету")
    public void shouldClickDealer() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsAllDealerIdEmpty")).update();

        basePageSteps.onDealerListingPage().getDealer(0).click();

        urlSteps.shouldNotDiffWith(dealerUrl);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию дилера")
    public void shouldClickDealerName() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsAllDealerIdEmpty")).update();

        basePageSteps.onDealerListingPage().getDealer(0).name().click();

        urlSteps.shouldNotDiffWith(dealerUrl);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию сети")
    public void shouldClickDealerNetName() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/AutoruBreadcrumbsStateNew"),
                stub("desktop/AutoruDealerAvilon"),
                stub("desktop/DealerNetAvilon")).update();

        basePageSteps.onDealerListingPage().getDealer(0).netUrl().click();

        urlSteps.testing().path(DEALER_NET).path("/avilon/").ignoreParam("geo_id").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Авто в продаже»")
    public void shouldClickSalesUrl() {
        mockRule.setStubs(stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsAllDealerIdEmpty")).update();

        basePageSteps.onDealerListingPage().getDealer(0).salesUrl().click();

        urlSteps.shouldNotDiffWith(dealerUrl);
    }
}