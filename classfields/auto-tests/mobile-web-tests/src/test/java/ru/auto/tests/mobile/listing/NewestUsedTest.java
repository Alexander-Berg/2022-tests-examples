package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.KRASNODAR;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.SortBar.SortBy.YEAR_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - блок б/у объявлений, когда нет новых")
@Feature(LISTING)
@Story(SPECIAL)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NewestUsedTest {

    private static final String MARK = "/geely/";
    private static final String MODEL = "/atlas/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

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
        mockRule.newMock().with("mobile/NewestUsedTest/SearchCarsBreadcrumbsGeelyAtlas",
                "mobile/NewestUsedTest/SearchCarsNew",
                "mobile/NewestUsedTest/SearchCarsUsed",
                "mobile/NewestUsedTest/SearchCarsCount").post();

        cookieSteps.setCookie("gradius", "0",
                format(".%s", urlSteps.getConfig().getBaseDomain()));
        urlSteps.testing().path(KRASNODAR).path(category).path(MARK).path(MODEL).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение заголовка блока")
    public void shouldSeeNewestUsedHeader() {
        basePageSteps.onListingPage().newestUsed().title().should(isDisplayed())
                .should(hasText("Geely Atlas почти как новые\nНовых Geely Atlas сейчас нет в продаже. " +
                        "Возможно, вам подойдут предложения с пробегом."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onListingPage().newestUsed().getItem(0).should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(MARK).path("/atlas/1085327978-72564886/")
                .addParam("geo_id", "35").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Смотреть все»")
    public void shouldClickAllUrl() {
        basePageSteps.hideElement(basePageSteps.onListingPage().fab());

        basePageSteps.onListingPage().newestUsed().button("Смотреть все").should(isDisplayed()).click();
        urlSteps.testing().path(KRASNODAR).path(category).path(MARK).path(MODEL).path(USED)
                .addParam("sort", YEAR_DESC.getAlias().toLowerCase()).shouldNotSeeDiff();
    }
}
