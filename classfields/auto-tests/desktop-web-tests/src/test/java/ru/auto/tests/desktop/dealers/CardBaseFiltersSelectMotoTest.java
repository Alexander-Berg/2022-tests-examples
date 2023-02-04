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

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры, селекты")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardBaseFiltersSelectMotoTest {

    private static final String PARAM_NAME = "Тип мотоцикла";
    private static final String PARAM_VALUE = "\u00a0\u00a0Allround ";
    private static final String PARAM_QUERY_NAME = "moto_type";
    private static final String PARAM_QUERY_VALUE = "ALLROUND";

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchMotoBreadcrumbs"),
                stub("desktop/SalonMoto"),
                stub("desktop/SearchMotoAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(MOTORCYCLE).path(ALL).path("motogarazh_moskva").open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Селект")
    public void shouldSelect() {
        basePageSteps.onDealerCardPage().filter().selectItem(PARAM_NAME, PARAM_VALUE);
        basePageSteps.onDealerCardPage().filter().select(PARAM_VALUE.replaceAll("\u00a0", "").trim())
                .waitUntil(isDisplayed()).click();

        urlSteps.path(SLASH).addParam(PARAM_QUERY_NAME, PARAM_QUERY_VALUE).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filter().should(isDisplayed());
    }
}