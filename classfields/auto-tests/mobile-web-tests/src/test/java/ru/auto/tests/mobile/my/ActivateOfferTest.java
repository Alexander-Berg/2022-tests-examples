package ru.auto.tests.mobile.my;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Notifications.STATUS_CHANGED;
import static ru.auto.tests.desktop.consts.Owners.NIKITABUGAEV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mobile.element.cardpage.OwnerControls.ACTIVATE;
import static ru.auto.tests.desktop.mobile.element.cardpage.OwnerControls.TAKE_OFF;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - активация объявления")
@Feature(LK)
@Story("Активация объявления")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ActivateOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String mockInactive;

    @Parameterized.Parameter(1)
    public String mockActivate;

    @Parameterized.Parameter(2)
    public String mockActive;

    @Parameterized.Parameter(3)
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"desktop/UserOffersCarsInactive", "desktop/UserOffersCarsActivate", "desktop/UserOffersCarsActive", CARS},
                {"desktop/UserOffersMotoInactive", "desktop/UserOffersMotoActivate", "desktop/UserOffersMotoActive", MOTO},
                {"desktop/UserOffersTrucksInactive", "desktop/UserOffersTrucksActivate", "desktop/UserOffersTrucksActive", TRUCKS},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/User"),
                stub("desktop/SessionAuthUser"),
                stub(mockInactive),
                stub(mockActivate)
        ).create();

        urlSteps.testing().path(MY).path(category).open();

        mockRule.overwriteStub(0, stub(mockActive));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NIKITABUGAEV)
    @DisplayName("Клик по кнопке «Активировать»")
    public void shouldClickActivateButton() {
        basePageSteps.onLkPage().getSale(0).button(ACTIVATE).click();
        basePageSteps.onLkPage().notifier(STATUS_CHANGED).waitUntil(isDisplayed());
        basePageSteps.onLkPage().getSale(0).button(ACTIVATE).waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button(TAKE_OFF).waitUntil(isDisplayed());

        basePageSteps.onLkPage().vas().should(isDisplayed());
    }
}
