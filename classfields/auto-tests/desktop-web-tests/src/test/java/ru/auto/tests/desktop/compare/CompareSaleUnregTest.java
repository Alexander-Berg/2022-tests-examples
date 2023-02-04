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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление объявления в сравнение")
@Feature(AutoruFeatures.COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CompareSaleUnregTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop-compare/UserCompareCarsFavoritePost",
                "desktop-compare/UserCompareCarsFavoriteDelete").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.with("desktop-compare/UserCompareCarsOneSale").update();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в сравнение на карточке")
    public void shouldAddToCompare() {
        basePageSteps.onCardPage().cardHeader().toolBar().compareButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Добавлено Перейти к сравнению 1 объявления"));
        basePageSteps.onCardPage().cardHeader().toolBar().compareDeleteButton().waitUntil(isDisplayed());
        urlSteps.refresh();
        basePageSteps.onCardPage().cardHeader().toolBar().compareDeleteButton().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из сравнения на карточке")
    public void shouldDeleteFromCompare() {
        basePageSteps.onCardPage().cardHeader().toolBar().compareButton().waitUntil(isDisplayed()).click();

        waitSomething(3, TimeUnit.SECONDS);
        mockRule.overwriteStub(3, "desktop-compare/UserCompareCarsEmpty");

        basePageSteps.onCardPage().cardHeader().toolBar().compareDeleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено"));
        basePageSteps.onCardPage().cardHeader().toolBar().compareButton().waitUntil(isDisplayed());
        urlSteps.refresh();
        basePageSteps.onCardPage().cardHeader().toolBar().compareButton().should(isDisplayed());
    }
}