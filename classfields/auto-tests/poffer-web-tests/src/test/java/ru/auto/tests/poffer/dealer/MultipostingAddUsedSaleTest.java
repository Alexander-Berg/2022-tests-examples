package ru.auto.tests.poffer.dealer;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MULTIPOSTING;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ADD_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Мультипостинг - добавление б/у объявления")
@Feature(POFFER)
@Story(MULTIPOSTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MultipostingAddUsedSaleTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/SearchCarsBreadcrumbsRid213",
                "desktop/Currencies",
                "desktop/ReferenceCatalogCarsAllOptions",
                "poffer/ReferenceCatalogCarsSuggestLifanSolano",
                "poffer/dealer/DealerInfoMultipostingEnabled",
                "poffer/dealer/UserDraftCarsDraftIdGetUsed",
                "poffer/dealer/UserDraftCarsUsed",
                "poffer/dealer/UserDraftCarsDraftIdPutUsed",
                "poffer/dealer/UserDraftCarsDraftIdPublishUsed",
                "poffer/dealer/UserModerationStatus",
                "poffer/ReferenceCatalogCarsParseOptionsEmpty").post();

        urlSteps.testing().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeMultiposting() {
        pofferSteps.onPofferPage().multiposting().should(hasText("Сайты для размещения объявления\n" +
                "Размещайте объявление сразу на нескольких площадках."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление б/у объявления - Авто.ру")
    public void shouldAddAutoruSale() {
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/cars_used_dealer_multiposting_autoru_offer.json")
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление б/у объявления - Авито")
    public void shouldAddAvitoSale() {
        pofferSteps.onPofferPage().multiposting().autoru().click();
        pofferSteps.onPofferPage().multiposting().avito().click();
        pofferSteps.onPofferPage().dealerVas().vases().waitUntil(not(isDisplayed()));
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/cars_used_dealer_multiposting_avito_offer.json")
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление б/у объявления - Дром")
    public void shouldAddDromSale() {
        pofferSteps.onPofferPage().multiposting().autoru().click();
        pofferSteps.onPofferPage().multiposting().drom().click();
        pofferSteps.onPofferPage().dealerVas().vases().waitUntil(not(isDisplayed()));
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/cars_used_dealer_multiposting_drom_offer.json")
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }
}
