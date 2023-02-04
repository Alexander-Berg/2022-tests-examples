package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Редирект на групповую карточку, если в листинге один сниппет")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetNewRedirectTest {

    private final static String MARK = "toyota";
    private final static String MODEL = "corolla";
    private final static String GROUP_ID = "/21491371-21491985/";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/SearchCarsBreadcrumbsToyota",
                "desktop/SearchCarsBreadcrumbsToyotaCorolla",
                "desktop/SearchCarsNewToyota",
                "desktop/SearchCarsNewToyotaCorollaOneSale",
                "desktop/SearchCarsNewToyotaCorollaOneSalePage1").post();
    }

    @Test
    @Ignore
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редирект на групповую карточку, если в листинге один сниппет")
    public void shouldRedirect() {
        urlSteps.testing().path(MOSKVA).path(category).path(MARK).path(MODEL).path(NEW).open();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(GROUP_ID)
                .addParam("from", "single_group_snippet_listing").shouldNotSeeDiff();
    }

    @Test
    @Ignore
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редирект на групповую карточку, если в листинге один сниппет (после выбора модели)")
    public void shouldRedirectAfterModelSelect() {
        urlSteps.testing().path(MOSKVA).path(category).path(MARK).path(NEW).open();
        basePageSteps.onListingPage().filter().selectItem("Модель", StringUtils.capitalize(MODEL));
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onListingPage().filter().resultsButton().click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(GROUP_ID)
                .addParam("from", "single_group_snippet_listing").shouldNotSeeDiff();
        basePageSteps.driver().navigate().back();
        urlSteps.testing().path(MOSKVA).path(category).path(MARK).path(MODEL).path(NEW)
                .addParam("is_page_redirected", "true").shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().should(isDisplayed());
    }
}