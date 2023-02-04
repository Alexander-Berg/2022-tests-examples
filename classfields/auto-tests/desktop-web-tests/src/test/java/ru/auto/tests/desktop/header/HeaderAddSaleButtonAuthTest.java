package ru.auto.tests.desktop.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.NATIX;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Шапка - меню «Продать» (легковые)")
@Feature(HEADER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HeaderAddSaleButtonAuthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps userSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATIX)
    @DisplayName("Клик по кнопке «Продать» (легковые)")
    public void shouldClickAddSaleButtonByUserCars() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll").post();

        urlSteps.testing().path(CARS).path(ALL).open();
        userSteps.onBasePage().header().button("Разместить бесплатно").click();
        urlSteps.testing().path(CARS).path(USED).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATIX)
    @DisplayName("Клик по кнопке «Продать» (комтранс)")
    public void shouldClickAddSaleButtonByUserTrucks() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchTrucksBreadcrumbsEmpty",
                "desktop/SearchTrucksAll").post();

        urlSteps.testing().path(TRUCK).path(ALL).open();
        userSteps.onBasePage().header().button("Разместить бесплатно").click();
        urlSteps.testing().path(TRUCKS).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATIX)
    @DisplayName("Клик по кнопке «Продать» (мото)")
    public void shouldClickAddSaleButtonByUserMoto() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchMotoBreadcrumbsEmpty",
                "desktop/SearchMotoAll").post();

        urlSteps.testing().path(MOTORCYCLE).path(ALL).open();
        userSteps.onBasePage().header().button("Разместить бесплатно").click();
        urlSteps.testing().path(MOTO).path(ADD).shouldNotSeeDiff();
    }
}
