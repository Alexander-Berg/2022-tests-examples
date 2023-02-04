package ru.auto.tests.desktopreviews.review;

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
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - счётчик просмотров")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CounterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "/uaz/patriot/2309645/4014660/"},
                {MOTO, "/motorcycle/honda/cb_400/8131894731002395272/"},
                {TRUCKS, "/truck/foton/aumark_10xx/4033391/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEW).path(category).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Счётчик просмотров")
    @Category({Regression.class})
    public void shouldIncreaseViewsCounter() {
        int counterBeforeRefresh = basePageSteps.onReviewPage().cover().getCounter();
        basePageSteps.refresh();

        assertThat("Счётчик просмотров не увеличился",
                basePageSteps.onReviewPage().cover().getCounter() == counterBeforeRefresh + 1);
    }
}