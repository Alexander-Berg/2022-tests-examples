package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Правильное отображение на фронте, когда ручка карточки отдает ошибку")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SaleErrorsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть 404, если ручка карточки отдает 404")
    public void shouldSee404() {
        mockRule.newMock().with("desktop/OfferCarsUsed404").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().titleTag()
                .should(hasAttribute("textContent", "Ошибка 404! Страница не найдена. - AUTO.RU"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть 500, если ручка карточки отдает 500 (редирект)")
    public void shouldSee500Redirect() {
        mockRule.newMock().with("desktop/OfferCarsUsed500").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().titleTag()
                .should(hasAttribute("textContent", "Ошибка 500! Сервис временно недоступен. - AUTO.RU"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть 500, если ручка карточки отдает 500 (без редиректа)")
    public void shouldSee500WithoutRedirect() {
        mockRule.newMock().with("desktop/OfferCarsUsed500").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().titleTag()
                .should(hasAttribute("textContent", "Ошибка 500! Сервис временно недоступен. - AUTO.RU"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть ошибку, если ручка телефонов не отвечает")
    public void shouldSeePhonesError() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/OfferCarsPhonesError").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().floatingContacts().callButton().click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Ошибка. Попробуйте снова"));
    }
}
