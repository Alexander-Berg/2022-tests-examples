package ru.yandex.general.userMenuPopup;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.USER_MENU_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(USER_MENU_FEATURE)
@Feature("Проверка ссылок")
@DisplayName("Проверка ссылок на разделы ЛК в меню юзера")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UserMenuLkLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Мои объявления", OFFERS},
                {"Избранное", FAVORITES},
                {"Настройки профиля", CONTACTS},
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.onBasePage().header().burger().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка ссылок на разделы ЛК в меню юзера")
    public void shouldSeeLkUrlsInUserMenuPopup() {
        basePageSteps.onListingPage().popup().link(name).should(
                hasAttribute(HREF, urlSteps.testing().path(MY).path(path).toString()));
    }

}
