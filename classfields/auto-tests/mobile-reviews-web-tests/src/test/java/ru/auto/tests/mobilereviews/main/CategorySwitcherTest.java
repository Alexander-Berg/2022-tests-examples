package ru.auto.tests.mobilereviews.main;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Переключатель категорий")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CategorySwitcherTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String categoryUrl;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {REVIEWS, "Мото", MOTO},
                {REVIEWS, "Коммерческие", TRUCKS},
                {"/reviews/moto/", "Легковые", ""}
        });
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по переключателю")
    public void shouldClickCategorySwitcher() {
        urlSteps.testing().path(startUrl).open();
        basePageSteps.onReviewsMainPage().categorySwitcher(category).should(isDisplayed()).click();
        urlSteps.testing().path(REVIEWS).path(categoryUrl).shouldNotSeeDiff();
        basePageSteps.onReviewsMainPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }
}
