package ru.auto.tests.desktop.dealers;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Секции 'Все/новые/с пробегом'")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardBaseFiltersSectionsTrucksTest {

    private static final String DEALER_CODE = "sollers_finans_moskva";

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

    @Parameterized.Parameter
    public String startSection;

    @Parameterized.Parameter(1)
    public String expectedSectionTitle;

    @Parameterized.Parameter(2)
    public String expectedSection;

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
//                {NEW, "Все", ALL},
                {ALL, "Новые", NEW},
                {ALL, "С пробегом", USED},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchTrucksBreadcrumbs"),
                stub("desktop/SalonTrucks"),
                stub("desktop/SearchTrucksAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(startSection).path(DEALER_CODE).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        basePageSteps.onDealerCardPage().filter().radioButton(expectedSectionTitle).should(isDisplayed()).click();

        urlSteps.testing().path(DILER_OFICIALNIY).path(TRUCK).path(expectedSection)
                .path(DEALER_CODE)
                .path(SLASH)
                .shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }
}