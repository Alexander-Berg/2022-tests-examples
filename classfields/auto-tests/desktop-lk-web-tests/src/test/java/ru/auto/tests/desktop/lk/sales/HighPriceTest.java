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

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Цена выше рыночной")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HighPriceTest {

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
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersCarsHighPrice"),
                stub("desktop/StatsPredict")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesPage().getSale(0).button("Цена выше рыночной").hover();
        basePageSteps.onLkSalesPage().popup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цена выше рыночной")
    public void shouldSeeHighPrice() {
        mockRule.setStubs(stub("desktop/UserOffersCarsPrice")).update();

        basePageSteps.onLkSalesPage().popup().should(hasText("Цена автомобиля на 625 500 ₽ выше средней рыночной\n" +
                "Ваша цена\n" +
                "Средняя цена похожих автомобилей\n≈ 4 324 500 ₽\n" +
                "Рекомендуем снизить цену — автомобили с высокой ценой продаются дольше.\n" +
                "Если вы считаете цену справедливой, рекомендуем указать в описании к объявлению причину " +
                "высокой цены и поднять объявление в поиске, чтобы больше людей обратило на него внимание.\n" +
                "Поднять в поиске за 597 ₽"));
        basePageSteps.onLkSalesPage().popup().button("≈ 4\u00a0324\u00a0500\u00a0₽").click();
        basePageSteps.onLkSalesPage().popup().button("Применить").click();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).should(hasText("Цена успешно изменена"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поднятие в поиске")
    public void shouldBuyFresh() {
        basePageSteps.onLkSalesPage().popup().button("Поднять в поиске за 597\u00a0₽").click();
        basePageSteps.onLkSalesPage().switchToBillingFrame();
        basePageSteps.onLkSalesPage().billingPopup().waitUntil(isDisplayed());
    }

}
