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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.PofferSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Дилер, баннер «Станьте проверенным собственником»")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ProvenOwnerTest {

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
                "poffer/dealer/UserModerationStatus").post();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(OFFER_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Дилер, не должно быть баннера «Станьте проверенным собственником»")
    public void shouldNotSeeProvenOwnerBanner() {
        pofferSteps.onPofferPage().provenOwnerBanner().should(not(isDisplayed()));
    }
}
