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
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Отчет")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PageLinksEgrnReportTest {

    private static final String EGRN_LOGIN = "bro";
    private static final String EGRN_PASSWORD = "chakp1";
    private static final String EGRN_REPORT_PATH = "egrn-report/b025ef62da1442e9817f7c94228ad6f9/";
    private static final String CHECK_OWNERS = "Проверьте собственников на\u00a0банкротство";

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
    public String block;

    @Parameterized.Parameter(1)
    public String link;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "{index} - {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Проверьте наличие открытого исполнительного производства",
                        "сайте Федеральной службы судебных приставов", "https://fssp.gov.ru/"},
                {CHECK_OWNERS, "на сайте Службы судебных приставов",
                        "https://fssp.gov.ru/"},
                {CHECK_OWNERS, "в Едином федеральном реестре сведений о\u00a0банкротстве",
                        "https://bankrot.fedresurs.ru/"},
                {CHECK_OWNERS, "на сайте Арбитражного суда",
                        "https://kad.arbitr.ru/"}
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
        basePageSteps.onEgrnReportPage().button("Ещё 7 рекомендаций").click();
        basePageSteps.onEgrnReportPage().block(block).link(link).should(hasHref(equalTo(url)));
    }
}
