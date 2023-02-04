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
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;

@DisplayName("Дилер - редактирование б/у объявления")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class EditUsedSaleTest {

    private static final String OFFER_ID = "/1076842087-f1e84/";
    private static final String OFFER_TEMPLATE = "offers/dealer_used_edit.json";

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
                "poffer/dealer/UserDraftCarsDraftIdPutUsedEdit",
                "poffer/dealer/UserDraftCarsDraftIdPublishUsed",
                "poffer/dealer/UserModerationStatus").post();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(OFFER_ID).open();
        pofferSteps.fillRun("10000");
        pofferSteps.fillPrice("500001");
        pofferSteps.fillExchange();
        pofferSteps.fillColor("FF2600");
        pofferSteps.fillPts("Дубликат");
        pofferSteps.fillOwnersCount("2");
        pofferSteps.fillVin("EDTGD18508S219350");
        pofferSteps.fillSts("6360269398");
        pofferSteps.fillComment("Отредактировано");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Дилер - редактирование б/у объявления")
    public void shouldEditSale() {
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveFormAndPay/",
                pofferHasJsonBody(OFFER_TEMPLATE)
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(OFFER_ID)
                .addParam(PAGE_FROM, EDIT_PAGE).shouldNotSeeDiff();
    }
}
