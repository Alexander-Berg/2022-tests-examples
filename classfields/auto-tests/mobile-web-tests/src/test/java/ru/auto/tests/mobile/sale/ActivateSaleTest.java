package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Notifications.STATUS_CHANGED;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.element.cardpage.OwnerControls.ACTIVATE;
import static ru.auto.tests.desktop.mobile.element.cardpage.OwnerControls.TAKE_OFF;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - активация")
@Feature(SALES)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActivateSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    @Parameterized.Parameter
    public String mockInactive;

    @Parameterized.Parameter(1)
    public String mockActivate;

    @Parameterized.Parameter(2)
    public String mockOffer;

    @Parameterized.Parameter(3)
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {3}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop/OfferCarsUsedUserOwnerInactive", "desktop/UserOffersCarsActivate",
                        "desktop/OfferCarsUsedUserOwner", CARS},
                {"desktop/OfferMotoUsedUserOwnerInactive", "desktop/UserOffersMotoActivate",
                        "desktop/OfferMotoUsedUserOwner", MOTORCYCLE,},
                {"desktop/OfferTrucksUsedUserOwnerInactive", "desktop/UserOffersTrucksActivate",
                        "desktop/OfferTrucksUsedUserOwner", TRUCKS},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub(mockInactive),
                stub(mockActivate)).create();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(1, stub(mockOffer));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KIRILL_PKR)
    @DisplayName("Активация на карточке объвления")
    public void shouldActivateSale() {
        basePageSteps.onCardPage().ownerControls().button(ACTIVATE).click();
        basePageSteps.onCardPage().notifier(STATUS_CHANGED).waitUntil(isDisplayed());
        basePageSteps.onCardPage().ownerControls().button(ACTIVATE).waitUntil(not(isDisplayed()));

        basePageSteps.onCardPage().ownerControls().button(TAKE_OFF).should(isDisplayed());
    }
}
