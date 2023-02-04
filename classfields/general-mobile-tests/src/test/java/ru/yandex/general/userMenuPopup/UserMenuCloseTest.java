package ru.yandex.general.userMenuPopup;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.USER_MENU_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(USER_MENU_FEATURE)
@DisplayName("Закрытие попапа меню юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class UserMenuCloseTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрытие попапа меню юзера")
    public void shouldSeeCloseUserMenuPopup() {
        basePageSteps.onBasePage().header().burger().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().closeFloatPopup().click();

        basePageSteps.onListingPage().popup().should(not(isDisplayed()));
    }

}
