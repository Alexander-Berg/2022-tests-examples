package ru.auto.tests.desktopreviews.filters;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры поиска - селекторы")
@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TechParamFilterCarsTest {

    private static final String MARK = "/audi/";
    private static final String MODEL = "/a3/";
    private static final String GENERATION = "/2305282/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Селект")
    @Parameterized.Parameter
    public String selectName;

    //@Parameter("Опция в селекте")
    @Parameterized.Parameter(1)
    public String selectItem;

    //@Parameter("Параметр")
    @Parameterized.Parameter(2)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "{0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Коробка", "Робот", "transmission", "%1$s=ROBOT"},
                {"Кузов", "Хэтчбек 5 дв.", "body_type", "%1$s=HATCHBACK_5_DOORS"},
                {"Год выпуска", "2012", "year_to", "%1$s=2012"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL)
                .path(GENERATION).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Выбор опции в селекте")
    public void shouldSelectItem() throws InterruptedException {
        basePageSteps.onReviewsListingPage().filters().selectItem(selectName, selectItem);
        urlSteps.replaceQuery(format(paramValue, param)).ignoreParam("sort").ignoreParam("year_from")
                .shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().filters().select(selectItem
                .replaceAll("\u00a0", "")).click();
        basePageSteps.onReviewsListingPage().waitForListingReload();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onReviewsListingPage().filters().select(selectItem
                .replaceAll("\u00a0", "")).should(isDisplayed());
    }
}