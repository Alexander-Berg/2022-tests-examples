package ru.yandex.arenda.admin.users;

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
import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.SEARCH_BUTTON;
import static ru.yandex.arenda.matcher.FindPatternMatcher.findPattern;
import static ru.yandex.arenda.steps.MainSteps.FIRST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1729")
@DisplayName("[Админка] Листинг пользователей")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UsersFilterPhoneNumberTest {

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

    @Parameterized.Parameter
    public String phone;

    @Parameterized.Parameters(name = "Ссылка на «{0}»")
    public static Collection<String> rentType() {
        return asList("904333", "+7 (904) 333-33-33", "+7904 333-33-33", "+79043333333");
    }

    @Test
    @DisplayName("Поиск по телефону")
    public void shouldSeeUsersByPhone() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USERS).open();
        lkSteps.onAdminListingPage().managerUserFilters().queryFilter().sendKeys(phone);
        lkSteps.onAdminListingPage().managerUserFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerUsersItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", phone).ignoreParam("page");
        lkSteps.onAdminListingPage().managerUsersItem().forEach(flat ->
                flat.phone().should(hasText(findPattern("\\+7 \\(904\\) 333-\\d{2}-\\d{2}"))));
    }
}
