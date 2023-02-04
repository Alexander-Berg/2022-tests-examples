package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления дилера - контакты под владельцем")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ContactsDealerInStockUrlTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String dealerCode;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "favorit_motors_ug_moskva"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedDealer").post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «... в наличии»")
    public void shouldClickInStockUrl() {
        basePageSteps.onCardPage().contacts().inStockUrl().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER).path(category).path(USED).path(dealerCode).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «... в наличии» в плавающей панели")
    public void shouldClickStickyBarInStockUrl() {
        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
        basePageSteps.onCardPage().stickyBar().inStockUrl().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER).path(category).path(USED).path(dealerCode).path("/").shouldNotSeeDiff();
    }
}