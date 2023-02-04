package ru.yandex.general.contacts;

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
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mobile.page.ContactsPage.NOTIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;

@Epic(CONTACTS_FEATURE)
@Feature("Ссылки на избранное")
@DisplayName("Ссылки на избранное")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ContactsFavoritesLinksTest {

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
    public String tabParamValue;

    @Parameterized.Parameters(name = "{index}. Ссылка «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Новое в избранных профилях", PROFILES_TAB_VALUE},
                {"Новые объявления в сохранённых поисках", SEARCHES_TAB_VALUE}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(CONTACTS).open();
        basePageSteps.onContactsPage().spanLink(NOTIFICATIONS).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки на избранное на странице «Настройки»")
    public void shouldSeeFavoritesLinksFromContactsPage() {
        basePageSteps.onContactsPage().wrapper(NOTIFICATIONS).link(name).should(hasAttribute(HREF,
                urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, tabParamValue).toString()));
    }

}
