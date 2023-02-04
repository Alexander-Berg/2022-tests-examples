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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.QueryParams.EDIT_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Мультипостинг - редактирование нового объявления")
@Feature(POFFER)
@Story(MULTIPOSTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MultipostingEditNewSaleTest {

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
                "poffer/dealer/UserOffersCarsOfferIdNew",
                "poffer/dealer/UserOffersCarsOfferIdEdit",
                "poffer/dealer/UserDraftCarsDraftIdGetNew",
                "poffer/dealer/UserDraftCarsDraftIdPutNewEdit",
                "poffer/dealer/UserDraftCarsDraftIdPublishNew",
                "poffer/dealer/UserModerationStatus",
                "poffer/ReferenceCatalogCarsParseOptionsEmpty").post();

        urlSteps.testing().path(CARS).path(NEW).path(EDIT).path(OFFER_ID).open();
        pofferSteps.fillPrice("500001");
        pofferSteps.fillCreditDiscount("10000");
        pofferSteps.fillInsuranceDiscount("10000");
        pofferSteps.fillTradeInDiscount("10000");
        pofferSteps.fillMaxDiscount("30000");
        pofferSteps.fillExchange();
        pofferSteps.fillColor("FF2600");
        pofferSteps.fillAvailability("На заказ");
        pofferSteps.fillVin("EDTGD18508S219350");
        pofferSteps.fillComment("Отредактировано");
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
    @DisplayName("Дилер - редактирование нового объявления")
    public void shouldEditSale() {
        pofferSteps.onPofferPage().multiposting().autoru().click();
        pofferSteps.onPofferPage().multiposting().avito().click();
        pofferSteps.onPofferPage().dealerVas().vases().waitUntil(not(isDisplayed()));
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody("offers/dealer_new_multiposting_avito_edit.json")
        ));
        urlSteps.testing().path(CARS).path(NEW).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, EDIT_PAGE).shouldNotSeeDiff();
    }
}
