package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Гараж")
@Story("Карточка своего автомобиля")
@Feature(AutoruFeatures.GARAGE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GarageCardUrlsTest {

    private static final String VIN_CARD_ID = "/1146321503/";

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

    //@Parameter("Название ссылки")
    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Отзывные кампании"},
                {"Потеря стоимости"},
                {"Рейтинг модели"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/GarageUserCardsVinPost"),
                stub("desktop/GarageUserCardVin"),
                stub("desktop/ReferenceCatalogCarsSuggestVolkswagenJetta"),
                stub("desktop/ReviewsAutoCarsCounterVolkswagenJetta7355324"),
                stub("desktop/ReviewsAutoCarsRatingVolkswagenJetta7355324"),
                stub("desktop/ReviewsAutoFeaturesCarsVolkswagenJetta7355324"),
                stub("desktop/ReviewsAutoListingVolkswagenJetta7355324")).create();

        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        long pageOffset = basePageSteps.getPageYOffset();
        basePageSteps.onGarageCardPage().leftColumn().button(title).click();
        urlSteps.shouldNotSeeDiff();
        waitSomething(3, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }
}
