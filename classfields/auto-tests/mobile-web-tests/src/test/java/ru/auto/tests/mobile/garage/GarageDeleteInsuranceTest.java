package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж")
@Story("Страхование")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageDeleteInsuranceTest {

    private static final String CARD_ID = "/1146321503/";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/GarageUserCardOsago",
                "desktop/GarageUserCardDeleteInsurance").post();

        urlSteps.testing().path(GARAGE).path(CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Удаление страховки")
    public void shouldDeleteInsurance() {
        basePageSteps.onGarageCardPage().insurances().list().get(0).editButton().click();
        basePageSteps.onGarageCardPage().popup().button("Редактировать").click();
        basePageSteps.onGarageCardPage().popup().button("Удалить").click();
        basePageSteps.onGarageCardPage().insuranceDeleteConfirmPopup().should(isDisplayed());

        mockRule.overwriteStub(2, "desktop/GarageUserCardVin");

        basePageSteps.onGarageCardPage().insuranceDeleteConfirmPopup().button("Удалить").click();

        basePageSteps.onGarageCardPage().insuranceDeleteConfirmPopup().should(not(isDisplayed()));
        basePageSteps.onGarageCardPage().popup().should(not(isDisplayed()));

        basePageSteps.onGarageCardPage().insurances().list().should(hasSize(0));
    }
}
