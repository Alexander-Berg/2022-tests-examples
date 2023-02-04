package ru.yandex.realty.menu;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MENU;

@Issue("VERTISTEST-1352")
@Feature(MENU)
@DisplayName("Закрытие меню")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class MenuCloseTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрытие меню")
    public void shouldSeeClosedMenu() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().menuButton().click();
        basePageSteps.onBasePage().menu().waitUntil(isDisplayed());
        basePageSteps.onBasePage().menu().closeCross().click();

        basePageSteps.onBasePage().menu().should(not(isDisplayed()));
        basePageSteps.onMobileMainPage().searchFilters().should(isDisplayed());

    }

}
