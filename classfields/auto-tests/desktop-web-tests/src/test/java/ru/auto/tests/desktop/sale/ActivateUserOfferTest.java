package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.card.CardOwnerPanel.PUBLISH;
import static ru.auto.tests.desktop.element.card.CardOwnerPanel.STATUS_ACTIVE;
import static ru.auto.tests.desktop.element.card.CardOwnerPanel.STATUS_INACTIVE;
import static ru.auto.tests.desktop.element.card.CardOwnerPanel.WITHDRAW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активация объявления")
@Feature(SALES)
@Story("Активация объявления")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(DesktopTestsModule.class)
public class ActivateUserOfferTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String mockInactive;

    @Parameterized.Parameter(1)
    public String mockActivate;

    @Parameterized.Parameter(2)
    public String category;

    @Parameterized.Parameter(3)
    public String mockActive;


    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop/OfferCarsUsedUserOwnerInactive", "desktop/UserOffersCarsActivate", CARS, "desktop/OfferCarsUsedUserOwner"},
                {"desktop/OfferMotoUsedUserOwnerInactive", "desktop/UserOffersMotoActivate", MOTORCYCLE, "desktop/OfferMotoUsedUserOwner"},
                {"desktop/OfferTrucksUsedUserOwnerInactive", "desktop/UserOffersTrucksActivate", TRUCKS, "desktop/OfferTrucksUsedUserOwner"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                mockInactive,
                mockActivate).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(1, mockActive);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KIRILL_PKR)
    @DisplayName("Активация объявления")
    public void shouldActivateSale() {
        basePageSteps.onCardPage().cardOwnerPanel().status().waitUntil(isDisplayed()).should(hasText(STATUS_INACTIVE));
        basePageSteps.onCardPage().cardOwnerPanel().button(PUBLISH).click();
        basePageSteps.onCardPage().cardOwnerPanel().status().waitUntil(hasText(STATUS_ACTIVE));

        basePageSteps.onCardPage().cardOwnerPanel().button(WITHDRAW).should(isDisplayed());
    }
}