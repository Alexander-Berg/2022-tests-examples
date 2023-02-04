package ru.yandex.realty.egrn.report;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Отчет")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HintPopupEgrnReportTest {

    private static final String EGRN_LOGIN = "bro";
    private static final String EGRN_PASSWORD = "chakp1";
    private static final String EGRN_REPORT_PATH = "egrn-report/b025ef62da1442e9817f7c94228ad6f9/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String hint;

    @Parameterized.Parameter(1)
    public String text;

    @Parameterized.Parameters(name = "{index} - {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Записей об\u00a0ипотеке не\u00a0найдено", "Если квартира в ипотеке,"},
                {"Арестов жилья не\u00a0найдено", "Государство имеет право отнять"},
                {"Записей об\u00a0аренде не\u00a0найдено ", "Если договор найма зарегистрирован"},
                {"Рента не\u00a0найдена", "При наличии такого обременения"},
                {"Квартира не\u00a0находится в\u00a0доверительном управлении", "Если квартиру продаёт управляющий,"},
                {"Запреты не\u00a0найдены", "При наличии запрета "},
                {"Информации о\u00a0наличии решения об\u00a0изъятии квартиры не\u00a0найдено", "Если в отношении"},
        });
    }

    @Before
    public void before() {
        passportSteps.login(EGRN_LOGIN, EGRN_PASSWORD);
        urlSteps.testing().path(EGRN_REPORT_PATH).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим переход из отчета в карточку оффера")
    public void shouldSeePathToLinks() {
        basePageSteps.moveCursorAndClick(basePageSteps.onEgrnReportPage().hint(hint));
        basePageSteps.onEgrnReportPage().popupVisible().should(hasText(containsString(text)));
    }
}
