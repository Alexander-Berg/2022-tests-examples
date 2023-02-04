package ru.auto.tests.mobile.reviews;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Вкладки в блоке «Рейтинг и отзывы» на карточке объявления")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewsSaleCarsTabsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String tabTitle;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Минусы"},
                {"Спорное"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/ReviewsAutoCarsCounter",
                "mobile/ReviewsAutoListingCars",
                "desktop/ReviewsAutoCarsRating",
                "desktop/ReviewsAutoFeaturesCars",
                "desktop/ReviewsAutoFeaturesCarsSnippet").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().button("Пожаловаться на объявление").hover();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Переключение вкладок")
    public void shouldSwitchTabs() {
        String activeTabFirstItemTitle = basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0).getText();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviewsPlusMinus().tab(tabTitle));
        basePageSteps.onCardPage().reviewsPlusMinus().tab(tabTitle)
                .waitUntil(hasClass(containsString("Radio_checked")));
        basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0)
                .waitUntil(not(hasText(activeTabFirstItemTitle)));
    }
}
