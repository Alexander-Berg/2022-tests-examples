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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Notifications.DELETED;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление объявления в сравнение, тип листинга «Карусель»")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CompareListingCarouselRegTest {

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
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsAll"),
                stub("desktop-compare/UserCompareCarsFavoritePost"),
                stub("desktop-compare/UserCompareCarsFavoriteDelete")
        ).create();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();

        mockRule.setStubs(
                stub("desktop-compare/UserCompareCarsOneSale")
        ).update();

        basePageSteps.onListingPage().getCarouselSale(0).waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().compareButton().waitUntil(isDisplayed()).hover().click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в сравнение, тип листинга «Карусель»")
    public void shouldAddToCompare() {
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Добавлено Перейти к сравнению 1 объявления"));
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().compareDeleteButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из сравнения, тип листинга «Карусель»")
    public void shouldDeleteFromCompare() {
        waitSomething(3, TimeUnit.SECONDS);
        mockRule.overwriteStub(5,
                stub("desktop-compare/UserCompareCarsEmpty"));

        basePageSteps.onListingPage().getCarouselSale(0).toolBar().compareDeleteButton().waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().notifier(DELETED).should(isDisplayed());
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().compareButton().should(isDisplayed());
    }

}
