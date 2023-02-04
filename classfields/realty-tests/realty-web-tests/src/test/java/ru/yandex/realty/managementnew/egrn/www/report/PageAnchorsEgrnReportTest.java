package ru.yandex.realty.managementnew.egrn.www.report;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Отчет")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PageAnchorsEgrnReportTest {

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
    private ManagementSteps managementSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String fragment;

    @Parameterized.Parameters(name = "{index} - {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Общие сведения", "general"},
                {"Собственники", "owners"},
                {"Обременения", "encumbrances"},
                {"Анализ", "analysis"},
                {"Расположение", "location"},
                {"Сведения о доме", "building"},
        });
    }

    @Before
    public void before() {
        passportSteps.login(EGRN_LOGIN, EGRN_PASSWORD);
        basePageSteps.resize(1400, 1600);
        urlSteps.testing().path(EGRN_REPORT_PATH).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим переход из отчета в карточку оффера")
    public void shouldSeePathToSections() {
        managementSteps.onEgrnReportPage().section(title).click();
        urlSteps.fragment(fragment).shouldNotDiffWithWebDriverUrl();
    }
}
