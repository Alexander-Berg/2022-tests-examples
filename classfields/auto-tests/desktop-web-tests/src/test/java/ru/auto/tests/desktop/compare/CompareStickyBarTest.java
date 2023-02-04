package ru.auto.tests.desktop.compare;

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
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравнение в плавающей панели на карточке")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CompareStickyBarTest {

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
        basePageSteps.setWideWindowSize();

        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop-compare/UserCompareCarsFavoritePost",
                "desktop-compare/UserCompareCarsFavoriteDelete").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.with("desktop-compare/UserCompareCarsOneSale").update();

        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
        basePageSteps.onCardPage().stickyBar().addToCompareButton().click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в сравнение")
    public void shouldAddToCompare() {
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Добавлено Перейти к сравнению 1 объявления"));
        basePageSteps.onCardPage().stickyBar().deleteFromCompareButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из сравнения")
    public void shouldDeleteFromCompare() {
        waitSomething(3, TimeUnit.SECONDS);
        mockRule.overwriteStub(3, "desktop-compare/UserCompareCarsEmpty");

        basePageSteps.onCardPage().stickyBar().deleteFromCompareButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Удалено"));
        basePageSteps.onCardPage().stickyBar().addToCompareButton().waitUntil(isDisplayed());
    }
}
