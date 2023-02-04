package ru.auto.tests.desktopreviews.breadcrumbs;

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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BREADCRUMBS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AutoruFeatures.REVIEWS)
@Story(BREADCRUMBS)
@DisplayName("Хлебные крошки в легковых")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BreadcrumbsCarsTest {

    private static final String MARK = "/subaru/";
    private static final String MODEL = "/impreza/";
    private static final String GENERATION = "/3492727/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String path;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/review/cars/subaru/impreza/3492727/4178932335117007625/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение хлебных крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onReviewPage().breadcrumbs().should(hasText("Продажа автомобилей \nОтзывы \nОтзывы об " +
                "автомобилях - Subaru \nОтзывы об автомобилях - Subaru Impreza \nI \nЗамена "));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Продажа автомобилей»")
    public void shouldClickSellUrl() {
        basePageSteps.onReviewPage().breadcrumbs().button("Продажа\u00a0автомобилей\u00a0").click();
        urlSteps.testing().path(MOSKVA).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Отзывы»")
    public void shouldClickReviewsUrl() {
        basePageSteps.onReviewPage().breadcrumbs().button("Отзывы\u00a0").click();
        urlSteps.testing().path(REVIEWS).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        basePageSteps.onReviewPage().breadcrumbs().button("Отзывы\u00a0об автомобилях -  Subaru\u00a0")
                .click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onReviewPage().breadcrumbs().button("Отзывы\u00a0об автомобилях -  Subaru Impreza\u00a0")
                .click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по поколению")
    public void shouldClickGeneration() {
        basePageSteps.onReviewPage().breadcrumbs().button("I\u00a0").click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }
}