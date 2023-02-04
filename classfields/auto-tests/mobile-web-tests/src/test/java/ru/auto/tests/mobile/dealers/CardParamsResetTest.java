package ru.auto.tests.mobile.dealers;

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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Карточка дилера - сброс параметров")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardParamsResetTest {

    private static final String DEALER_CODE = "/rolf_ugo_vostok_avtomobili_s_probegom_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL},
                {NEW},
                {USED}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(DILER).path(CARS).path(section).path(DEALER_CODE)
                .addParam("price_from", "100000").open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().paramsButton());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Сбросить»")
    public void shouldClickResetButton() {
        basePageSteps.onDealerCardPage().paramsPopup().resetButton().click();
        urlSteps.testing().path(DILER).path(CARS).path(section).path(DEALER_CODE).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
    }
}
