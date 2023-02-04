package ru.yandex.arenda.admin.contract;

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
import static ru.yandex.arenda.constants.UriPath.FLATS;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1719")
@DisplayName("[Админка] Тесты на страницу договора")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ContractsStatusTest {

    private static final String STATUS = "status";

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
    public String title;

    @Parameterized.Parameter(1)
    public String status;

    @Parameterized.Parameters(name = "Статус «{0}»")
    public static Collection<Object[]> rentType() {
        return asList(new Object[][]{
                {"Ожидает подтверждения", "waiting_for_confirmation"},
                {"Отменено", "cancelled_without_signing"},
        });
    }

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Проверяем наличие кнопки создания договора")
    public void shouldSeeTakeContract() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLATS).queryParam(STATUS, status).open();
        lkSteps.onAdminListingPage().managerFlatsItemFirst().link().click();
        lkSteps.onAdminFlatPage().button("Договоры").click();
        lkSteps.onAdminFlatPage().button("Создать договор").should(isDisplayed());
        lkSteps.onAdminFlatPage().h2().should(hasText("Нет актуального договора"));
    }
}
