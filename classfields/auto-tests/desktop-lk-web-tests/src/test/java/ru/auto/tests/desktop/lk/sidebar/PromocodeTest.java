package ru.auto.tests.desktop.lk.sidebar;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.element.lk.SalesListItem.ACTIVATE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Промокод")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PromocodeTest {

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
                "desktop/User",
                "desktop-lk/UserOffersCarsEmpty",
                "desktop-lk/ApiServiceAutoruUsersFeatureUserAutoruCommonTwoPromocodes").post();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ввод корректного промокода")
    public void shouldEnterPromocode() {
        basePageSteps.onLkSalesPage().sidebar().promocodeBlock()
                .should(hasText("Промокоды\nАктивировать\nРазмещение\n1 шт.\n0 ₽\nАктивно до 00:00, 1 янв. 2037\n" +
                        "Поднятие в топ\n1 шт.\n0 ₽\nАктивно до 00:00, 1 янв. 2037"));

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/UserOffersCarsEmpty",
                "desktop-lk/ApiServiceAutoruUsersFeatureUserAutoruCommonPost",
                "desktop-lk/ApiServiceAutoruUsersFeatureUserAutoruCommonThreePromocodes").post();

        basePageSteps.onLkSalesPage().sidebar().promocodeBlock().input().sendKeys("12345");
        basePageSteps.onLkSalesPage().sidebar().promocodeBlock().button(ACTIVATE).click();
        basePageSteps.onLkSalesPage().sidebar().promocodeBlock()
                .waitUntil(hasText("Промокоды\nАктивировать\nРазмещение\n1 шт.\n0 ₽\nАктивно до 00:00, 1 янв. 2037\n" +
                        "Поднятие в топ\n1 шт.\n0 ₽\nАктивно до 00:00, 1 янв. 2037\nПоднятие в топ\n1 шт.\n0 ₽\n" +
                        "Активно до 00:00, 1 янв. 2037"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ввод некорректного промокода")
    public void shouldSeeErrorMessage() {
        basePageSteps.onLkSalesPage().sidebar().promocodeBlock().input().sendKeys("123");
        basePageSteps.onLkSalesPage().sidebar().promocodeBlock().button(ACTIVATE).click();
        basePageSteps.onLkSalesPage().sidebar().promocodeBlock().errorMessage()
                .waitUntil(hasText("Такого промокода не существует"));
    }
}
