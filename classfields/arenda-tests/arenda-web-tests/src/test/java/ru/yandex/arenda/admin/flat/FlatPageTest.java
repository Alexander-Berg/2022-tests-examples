package ru.yandex.arenda.admin.flat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.pages.AdminFlatPage.FLAT_ADDRESS;
import static ru.yandex.arenda.pages.AdminFlatPage.FLAT_STATUS;
import static ru.yandex.arenda.pages.AdminFlatPage.OWNER_APPLICATION_EMAIL;
import static ru.yandex.arenda.pages.AdminFlatPage.OWNER_APPLICATION_NAME;
import static ru.yandex.arenda.pages.AdminFlatPage.OWNER_APPLICATION_PHONE;
import static ru.yandex.arenda.pages.AdminFlatPage.OWNER_APPLICATION_SURNAME;
import static ru.yandex.arenda.pages.AdminFlatPage.SAVE_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@Link("https://st.yandex-team.ru/VERTISTEST-1730")
@DisplayName("[Админка] Страница квартиры")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class FlatPageTest {

    private static final String DISABLED_ATTRIBUTE = "disabled";
    private static final String TRUE_VALUE = "true";
    private static final String CREATED_FLAT_PATH = "/f5504803b5f04b1381535d149d5e6942/";

    private static final String ADDRESS_VALUE = "Санкт-Петербург, Пискарёвский проспект, 1, кв. 321";
    private static final String NAME_VALUE = "Георгий";
    private static final String SURNAME_VALUE = "Литягин";
    private static final String PHONE_VALUE = "+7 (911) 159-77-92";
    private static final String EMAIL_VALUE = "gosha.fizik3@yandex.ru";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Все поля кроме «Желаемой арендной платы» недоступны для редактирования")
    public void shouldSeeDisabledElements() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(CREATED_FLAT_PATH).open();

        asList(FLAT_ADDRESS, FLAT_STATUS, OWNER_APPLICATION_NAME, OWNER_APPLICATION_SURNAME, OWNER_APPLICATION_PHONE,
                OWNER_APPLICATION_EMAIL)
                .forEach(id ->
                        lkSteps.onAdminFlatPage().byId(id).should(hasAttribute(DISABLED_ATTRIBUTE, TRUE_VALUE)));

        lkSteps.onAdminFlatPage().button(SAVE_BUTTON).should(hasAttribute(DISABLED_ATTRIBUTE, TRUE_VALUE));
    }

    @Test
    @DisplayName("Проверяем значения в полях")
    public void shouldSeeValues() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(CREATED_FLAT_PATH).open();

        lkSteps.onAdminFlatPage().byId(FLAT_ADDRESS).should(hasValue(ADDRESS_VALUE));
        lkSteps.onAdminFlatPage().byId(OWNER_APPLICATION_NAME).should(hasValue(NAME_VALUE));
        lkSteps.onAdminFlatPage().byId(OWNER_APPLICATION_SURNAME).should(hasValue(SURNAME_VALUE));
        lkSteps.onAdminFlatPage().byId(OWNER_APPLICATION_PHONE).should(hasValue(PHONE_VALUE));
        lkSteps.onAdminFlatPage().byId(OWNER_APPLICATION_EMAIL).should(hasValue(EMAIL_VALUE));
    }
}
