package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Лист продажи")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsHistoryCarsTest {

    private final static String OFFER_ID_PATH = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersCarsActive",
                "desktop/UserOffersCarsCallHistory").post();

        basePageSteps.setWideWindowSize();

        urlSteps.testing().path(MY).path(RESELLER).path(CARS).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редактируем цену")
    public void shouldEditPrice() {
        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().moreIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().popup().button("История звонков").should(isDisplayed()).click();

        basePageSteps.onLkResellerSalesPage().callHistoryPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().callHistoryPopup().phonesList().waitUntil(hasSize(greaterThan(0)));
    }
}
