package ru.yandex.arenda.admin.flat;

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
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.element.lk.admin.ManagerFlatFilters.SEARCH_BUTTON;
import static ru.yandex.arenda.matcher.FindPatternMatcher.findPattern;
import static ru.yandex.arenda.steps.MainSteps.FIRST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1728")
@DisplayName("[Админка] Листинг квартир")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FlatFilterPhoneNumberTest {

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
    public String name;

    @Parameterized.Parameter(1)
    public String phone;

    @Parameterized.Parameters(name = "Ссылка на «{0}»")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"Частичный номер", "965024"},
                {"Полный со скобками", "+7 (965) 024-79-44"},
                {"Полный без скобок", "+7965 024-79-44"},
                {"Полный без знаков", "+79650247944"},
        });
    }

    @Test
    @DisplayName("Поиск по телефону")
    public void shouldSeeByPhone() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).open();
        lkSteps.onAdminListingPage().managerFlatFilters().queryFilter().sendKeys(phone);
        lkSteps.onAdminListingPage().managerFlatFilters().button(SEARCH_BUTTON).click();
        lkSteps.onAdminListingPage().managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(isDisplayed()));
        urlSteps.queryParam("query", phone).ignoreParam("status");
        lkSteps.onAdminListingPage().managerFlatsItem().forEach(flat ->
                flat.should(hasText(findPattern("\\+7 \\(965\\) 024-\\d{2}-\\d{2}"))));
    }
}
