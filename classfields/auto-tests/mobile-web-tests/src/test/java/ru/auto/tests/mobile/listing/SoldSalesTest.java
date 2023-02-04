package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffersCarsExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - отображение проданного автомобиля")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SoldSalesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    //@Parameter("Категория ТС")
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
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsUsedWithRevokedOnly"),
                stub("mobile/SearchCarsUsed").withPredicateType(DEEP_EQUALS)
                        .withResponseBody(searchOffersCarsExample().setStatusOffer("INACTIVE").getBody())).create();

        urlSteps.testing().path(MOSKVA).path(category).path(USED).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение проданного автомобиля")
    public void shouldSeeSoldSale() {
        basePageSteps.onListingPage().soldSalesList().should(hasSize(1)).get(0).soldBadge()
                .should(anyOf(
                        hasText(matchesPattern("Этот автомобиль продан за \\d+ (минут|минуту|минуты|дня|дней)")),
                        hasText("Этот автомобиль продан")));
    }
}
