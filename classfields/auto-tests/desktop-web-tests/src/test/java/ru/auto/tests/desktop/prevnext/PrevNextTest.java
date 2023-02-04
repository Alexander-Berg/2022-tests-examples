package ru.auto.tests.desktop.prevnext;

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
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Переключение на предыдущее/следующее объявление на карточке")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PrevNextTest {

    private String firstOfferLink;
    private String secondOfferLink;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Тип транспорта")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, USED},
                {TRUCK, USED},
                {MOTORCYCLE, NEW}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        waitSomething(6, TimeUnit.SECONDS); //ждем запрос за списком объявлений в листинге
        firstOfferLink = basePageSteps.onListingPage().getSale(0).nameLink().getAttribute("href");
        secondOfferLink = basePageSteps.onListingPage().salesList().get(1).nameLink().waitUntil(isDisplayed())
                .getAttribute("href");

        basePageSteps.onListingPage().getSale(0).nameLink().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().footer().hover();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Переключение на следующее/предыдущее объявление")
    public void clickPrevNext() {
        basePageSteps.onCardPage().stickyBar().next().waitUntil(isDisplayed()).click();
        urlSteps.fromUri(secondOfferLink).shouldNotSeeDiff();
        basePageSteps.onCardPage().cardHeader().should(isDisplayed());
        basePageSteps.onCardPage().footer().hover();
        basePageSteps.onCardPage().stickyBar().prev().waitUntil(isDisplayed()).click();
        urlSteps.fromUri(firstOfferLink).shouldNotSeeDiff();
        basePageSteps.onCardPage().cardHeader().should(isDisplayed());
    }
}
