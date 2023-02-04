package ru.auto.tests.desktop.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.QueryParams.OWNER;
import static ru.auto.tests.desktop.consts.QueryParams.POPUP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.page.GaragePage.OWNER_CHECK_POPUP_TEXT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап проверенного собственника без авторизации")
@Epic(AutoruFeatures.GARAGE)
@Feature("Попап проверенного собственника")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageOwnerCheckPopupUnauthorizedTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth")
        ).create();

        urlSteps.testing().path(GARAGE).addParam(POPUP, OWNER).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап проверенного собственника для незалогина")
    public void shouldSeeOwnerCheckPopup() {
        basePageSteps.onGaragePage().ownerCheckPopup().waitUntil(isDisplayed()).should(hasText(OWNER_CHECK_POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап проверенного собственника закрывается по крестику")
    public void shouldCloseOwnerCheckPopup() {
        basePageSteps.onGaragePage().ownerCheckPopup().waitUntil(isDisplayed());
        basePageSteps.onGaragePage().ownerCheckPopup().closeIcon().click();

        basePageSteps.onGaragePage().ownerCheckPopup().should(not(isDisplayed()));
    }

}
