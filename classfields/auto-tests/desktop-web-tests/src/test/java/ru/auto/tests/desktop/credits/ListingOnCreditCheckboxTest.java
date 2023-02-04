package ru.auto.tests.desktop.credits;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CREDITS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Чекбокс «В кредит»")
@Feature(CREDITS)
@Story(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingOnCreditCheckboxTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выставляем чекбокс «В кредит»")
    public void shouldTurnCheckboxOn() {
        basePageSteps.onListingPage().filter().checkbox("В кредит").click();
        urlSteps.path(ON_CREDIT).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().waitForListingReload();
        basePageSteps.onListingPage().actualSalesList().subList(0, 5).forEach(sale -> {
            sale.creditPrice().should(isDisplayed());
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Снимаем чекбокс «В кредит»")
    public void shouldTurnCheckboxOff() {
        urlSteps.addParam("on_credit", "true").open();
        basePageSteps.onListingPage().filter().checkbox("В кредит").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(url).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сброс параметра при переходе в новые")
    public void shouldResetParamInNew() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(url).addParam("on_credit", "true").open();
        basePageSteps.onListingPage().filter().radioButton("Новые").click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).shouldNotSeeDiff();
    }
}
