package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.element.lk.SalesListItem.ACTIVATE;
import static ru.auto.tests.desktop.element.lk.SalesListItem.DEACTIVATE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Снятие с продажи")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DeactivateSaleTrucksTest {

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
                stub().withGetDeepEquals(USER_OFFERS_TRUCKS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.TRUCKS.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersTrucksActive"),
                stub("desktop-lk/UserOffersTrucksHide")
        ).create();

        urlSteps.testing().path(MY).path(TRUCKS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        mockRule.overwriteStub(2, stub("desktop-lk/UserOffersTrucksInactive"));

        basePageSteps.onLkSalesPage().getSale(0).button(DEACTIVATE).click();
        basePageSteps.onLkSalesPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkSalesPage().soldPopup().button("Снять с продажи").click();
        basePageSteps.onLkSalesPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onLkSalesPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesPage().reviewsPromo().waitUntil(not(isDisplayed()));
        basePageSteps.onLkSalesPage().getSale(0).waitUntil(hasText("Ford Courier\n75 000 ₽\nСнято с продажи\n" +
                ACTIVATE));
    }
}
