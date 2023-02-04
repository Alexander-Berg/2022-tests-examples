package ru.auto.tests.mobilereviews.main;

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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Главная отзывов")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {MOTO},
                {TRUCKS}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(category).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по самому полезному отзыву")
    @Category({Regression.class})
    public void shouldClickMostUsefulReview() {
        steps.onReviewsMainPage().topReview("Самый полезный").click();
        steps.onReviewPage().titleTag()
                .should(hasAttribute("textContent", startsWith("Отзыв владельца ")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по самому обсуждаемому отзыву")
    @Category({Regression.class})
    public void shouldClickMostDiscussedReview() {
        steps.onReviewsMainPage().topReview("Самый обсуждаемый").click();
        steps.onReviewPage().titleTag()
                .should(hasAttribute("textContent", startsWith("Отзыв владельца ")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по автору отзыва")
    @Category({Regression.class})
    public void shouldClickReviewAuthor() {
        steps.onReviewsMainPage().getReview(0).authorUrl().click();
        steps.onReviewPage().titleTag()
                .should(hasAttribute("textContent", "Страница пользователя"));
    }
}
