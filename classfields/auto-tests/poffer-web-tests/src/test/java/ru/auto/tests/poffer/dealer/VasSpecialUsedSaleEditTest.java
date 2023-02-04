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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.EDIT_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;

@DisplayName("Дилер - покупка услуги «Спецпредложение» при редактировании б/у объявления")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class VasSpecialUsedSaleEditTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";
    private static final String OFFER_TEMPLATE = "offers/cars_used_dealer_special_edit_offer.json";

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
                "poffer/dealer/UserOffersCarsOfferIdUsed",
                "poffer/dealer/UserOffersCarsOfferIdEdit",
                "poffer/dealer/UserDraftCarsDraftIdGetUsed",
                "poffer/dealer/UserDraftCarsUsed",
                "poffer/dealer/UserDraftCarsDraftIdPutUsedSpecial",
                "poffer/dealer/UserDraftCarsDraftIdPublishUsedSpecial",
                "poffer/dealer/UserModerationStatus").post();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(OFFER_ID).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Дилер - покупка услуги «Спецпредложение» при редактировании б/у объявления")
    public void shouldBuy() {
        pofferSteps.onPofferPage().dealerVas().vas("Спецпредложение").click();
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody(OFFER_TEMPLATE)
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID).addParam(PAGE_FROM, EDIT_PAGE)
                .shouldNotSeeDiff();
    }
}
