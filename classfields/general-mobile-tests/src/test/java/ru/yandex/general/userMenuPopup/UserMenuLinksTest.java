package ru.yandex.general.userMenuPopup;

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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.USER_MENU_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(USER_MENU_FEATURE)
@Feature("Проверка ссылок")
@DisplayName("Проверка ссылок в меню юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class UserMenuLinksTest {

    private static final String HELP_LINK = "https://yandex.ru/support/o-mobile/";

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
        basePageSteps.onBasePage().header().burger().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка ссылки «Разместить объявление» в меню юзера")
    public void shouldSeeFormUrlInUserMenuPopup() {
        basePageSteps.onListingPage().popup().link("Разместить объявление").should(
                hasAttribute(HREF, urlSteps.testing().path(FORM).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка ссылки «Помощь» в меню юзера")
    public void shouldSeeHelpUrlInUserMenuPopup() {
        basePageSteps.onListingPage().popup().link("Помощь").should(
                hasAttribute(HREF, HELP_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка ссылки «Магазинам» в меню юзера")
    public void shouldSeeForShopsUrlInUserMenuPopup() {
        basePageSteps.onListingPage().popup().link("Магазинам").should(
                hasAttribute(HREF, "https://o.yandex.ru/b2b?from=touchmenu"));
    }

}
