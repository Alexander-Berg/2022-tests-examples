package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMOCODES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO_CODES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - промокоды")
@Feature(LK)
@Story(PROMOCODES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class PromocodesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser").post();

        urlSteps.testing().path(MY).path(PROMO_CODES).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Активация промокода")
    public void shouldActivatePromocode() {
        mockRule.with("desktop/ApiServiceAutoruUsersPromocodeUserAutoruCommonPost",
                "desktop/ApiServiceAutoruUsersPromocodeUserAutoruCommon").update();

        basePageSteps.onLkPromocodesPage().input("Введите промокод", "123");
        basePageSteps.onLkPromocodesPage().button("Активировать").click();
        basePageSteps.onLkPromocodesPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Промокод активирован \uD83D\uDC4C"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Несуществующий промокод")
    public void shouldSeeNotExistPromoCodeError() {
        mockRule.with("desktop/ApiServiceAutoruUsersPromocodeUserAutoruCommonNotExist").update();

        basePageSteps.onLkPromocodesPage().input("Введите промокод", "123");
        basePageSteps.onLkPromocodesPage().button("Активировать").click();
        basePageSteps.onLkPromocodesPage().errorMessage().waitUntil(hasText("Такого промокода не существует"));
    }

}
