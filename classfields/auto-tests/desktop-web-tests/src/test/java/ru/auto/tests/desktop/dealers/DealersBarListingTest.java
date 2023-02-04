package ru.auto.tests.desktop.dealers;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок дилеров")
@Feature(AutoruFeatures.DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealersBarListingTest {

    private static final Integer DEALERS_CNT = 4;
    private static final String MARK = "/toyota/";

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

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/moskva/cars/toyota/all/"},
                {"/moskva/cars/toyota/corolla/all/"}
        });
    }

    @Before
    public void before() throws InterruptedException {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsToyota"),
                stub("desktop/SearchCarsBreadcrumbsToyotaCorolla"),
                stub("desktop/SearchCarsMark"),
                stub("desktop/SearchCarsMarkModel"),
                stub("desktop/DealersMark")
        ).create();

        urlSteps.testing().path(url).open();
        basePageSteps.onListingPage().footer().hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.scrollUp(1000);
        basePageSteps.onListingPage().dealersBlock().dealersList().waitUntil(hasSize(DEALERS_CNT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickHeaderUrl() {
        basePageSteps.onListingPage().dealersBlock().headerUrl().hover();
        basePageSteps.scrollUp(50);
        basePageSteps.onListingPage().dealersBlock().headerUrl().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(ALL)
                .addParam("from", "listing-mini-listing-list").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все дилеры»")
    public void shouldClickAllDealersUrl() {
        basePageSteps.onListingPage().dealersBlock().allDealersButton().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(ALL)
                .addParam("from", "listing-mini-listing-list").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по конкретному дилеру")
    public void shouldClickDealer() {
        basePageSteps.onListingPage().dealersBlock().getDealer(0).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path("/toyota_centr_bitca_moskva/").path(MARK)
                .addParam("from", "listing-mini-listing-list").shouldNotSeeDiff();
    }
}