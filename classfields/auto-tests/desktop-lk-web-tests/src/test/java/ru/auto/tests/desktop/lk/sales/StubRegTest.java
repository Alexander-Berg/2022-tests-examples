package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Заглушка под зарегом")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StubRegTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @com.google.inject.Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL, "/cars/used/"},
                {CARS, "/cars/used/"},
                {MOTO, MOTO},
                {TRUCKS, TRUCKS}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserOffersAllEmpty"),
                stub("desktop/UserOffersCarsEmpty"),
                stub("desktop/UserOffersTrucksEmpty"),
                stub("desktop/UserOffersMotoEmpty")
        ).create();

        urlSteps.testing().path(MY).path(startUrl).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Добавить объявление»")
    public void shouldClickSellButton() {
        basePageSteps.onLkSalesPage().stub().button("Добавить объявление").click();

        urlSteps.testing().path(category).path(ADD).shouldNotSeeDiff();
    }

}
