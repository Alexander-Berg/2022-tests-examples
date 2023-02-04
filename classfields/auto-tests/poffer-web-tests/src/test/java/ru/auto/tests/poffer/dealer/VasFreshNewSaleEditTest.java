package ru.auto.tests.poffer.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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

import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.QueryParams.EDIT_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;

@DisplayName("Дилер - покупка услуги «Поднятие в поиске» при редактировании нового объявления")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VasFreshNewSaleEditTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";
    private static final String OFFER_TEMPLATE = "offers/cars_new_dealer_fresh_edit_offer.json";

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
                "poffer/dealer/UserOffersCarsOfferIdNew",
                "poffer/dealer/UserOffersCarsOfferIdEdit",
                "poffer/dealer/UserDraftCarsDraftIdGetNew",
                "poffer/dealer/UserDraftCarsNew",
                "poffer/dealer/UserDraftCarsDraftIdPutNewFresh",
                "poffer/dealer/UserDraftCarsDraftIdPublishNewFresh",
                "poffer/dealer/UserModerationStatus",
                "poffer/ReferenceCatalogCarsParseOptionsEmpty").post();

        urlSteps.testing().path(CARS).path(NEW).path(EDIT).path(OFFER_ID).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Покупка услуги «Поднятие в поиске»")
    public void shouldBuyFresh() {
        pofferSteps.onPofferPage().dealerVas().vas("Поднятие в поиске").click();
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody(OFFER_TEMPLATE)
        ));
        urlSteps.testing().path(CARS).path(NEW).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, EDIT_PAGE).shouldNotSeeDiff();
    }
}
