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
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AutoruFeatures.REVIEWS)
@Story(BREADCRUMBS)
@DisplayName("Хлебные крошки в комтрансе")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BreadcrumbsCommercialTest {

    private static final String MARK = "/maz/";
    private static final String MODEL = "/5434/";

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
                {"/review/trucks/truck/maz/5434/4028401/"}
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
        basePageSteps.onReviewPage().breadcrumbs().should(hasText("Продажа коммерческого транспорта \nОтзывы \n" +
                "Отзывы о коммерческом транспорте - грузовики МАЗ \nОтзывы о коммерческом транспорте - грузовики МАЗ" +
                " 5434 \n\"Чудо автомобиль\" "));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Продажа коммерческого транспорта»")
    public void shouldClickSellUrl() {
        basePageSteps.onReviewPage().breadcrumbs().button("Продажа\u00a0коммерческого транспорта\u00a0").click();
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
        basePageSteps.onReviewPage().breadcrumbs()
                .button("Отзывы\u00a0о коммерческом транспорте - грузовики МАЗ\u00a0").click();
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path(MARK).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onReviewPage().breadcrumbs()
                .button("Отзывы\u00a0о коммерческом транспорте - грузовики МАЗ 5434\u00a0").click();
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }
}