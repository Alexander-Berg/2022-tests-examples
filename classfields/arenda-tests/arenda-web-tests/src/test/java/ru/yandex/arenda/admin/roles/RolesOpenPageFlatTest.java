package ru.yandex.arenda.admin.roles;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.pages.AdminFlatPage.CONCIERGE_COMMENT_ID;
import static ru.yandex.arenda.steps.UrlSteps.ADMIN_READ_ONLY_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.ADMIN_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.CONCIERGE_MANAGER_EXTENDED_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.CONCIERGE_MANAGER_ROLE_VALUE;
import static ru.yandex.arenda.steps.UrlSteps.DEGRADATION_ROLE_PARAM;
import static ru.yandex.arenda.steps.UrlSteps.ROLE_PARAM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2058")
@DisplayName("Админка. Комментарии. Открытие страниц")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RolesOpenPageFlatTest {

    private static final String TEST_FLAT_ID = "/5d1738fbaac04800a47994dce4eb17b7/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String user;

    @Parameterized.Parameter(1)
    public String role;

    @Parameterized.Parameters(name = "Страница «{0} ({1})»")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"Роль 1", CONCIERGE_MANAGER_ROLE_VALUE},
                {"Роль 2", CONCIERGE_MANAGER_EXTENDED_ROLE_VALUE},
                {"Роль 3", ADMIN_READ_ONLY_VALUE}
        });
    }

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Видим открытие юзера страницы квартиры под ролью")
    public void shouldSeeOpenedPage() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(TEST_FLAT_ID)
                .queryParam(DEGRADATION_ROLE_PARAM, ADMIN_ROLE_VALUE).queryParam(ROLE_PARAM, role).open();
        lkSteps.onAdminFlatPage().textAreaId(CONCIERGE_COMMENT_ID).should(isDisplayed());
    }
}
