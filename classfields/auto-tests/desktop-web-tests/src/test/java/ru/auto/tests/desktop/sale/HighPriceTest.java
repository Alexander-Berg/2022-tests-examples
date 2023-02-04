package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - цена выше рыночной")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HighPriceTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
                "desktop/OfferCarsUsedUserOwnerHighPrice",
                "desktop/StatsPredict").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().cardHeader().price().button("Цена выше рыночной").hover();
        basePageSteps.onCardPage().pricePopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цена выше рыночной")
    public void shouldSeeHighPrice() {
        mockRule.with("desktop/UserOffersCarsPrice").update();

        basePageSteps.onCardPage().pricePopup().should(hasText("Цена автомобиля на 625 500 ₽ выше средней рыночной\n" +
                "Ваша цена\n" +
                "Средняя цена похожих автомобилей\n≈ 4 324 500 ₽\n" +
                "Рекомендуем снизить цену — автомобили с высокой ценой продаются дольше.\n" +
                "Если вы считаете цену справедливой, рекомендуем указать в описании к объявлению причину " +
                "высокой цены и поднять объявление в поиске, чтобы больше людей обратило на него внимание.\n" +
                "Поднять в поиске за 597 ₽"));
        basePageSteps.onCardPage().pricePopup().button("≈ 4\u00a0324\u00a0500\u00a0₽").click();
        basePageSteps.onCardPage().pricePopup().button("Применить").click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Цена успешно изменена"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поднятие в поиске")
    public void shouldBuyFresh() {
        basePageSteps.onCardPage().pricePopup().button("Поднять в поиске за 597\u00a0₽").click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
    }
}