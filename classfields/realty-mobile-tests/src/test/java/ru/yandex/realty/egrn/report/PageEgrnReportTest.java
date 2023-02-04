package ru.yandex.realty.egrn.report;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.Pages.OTSENKA_KVARTIRY;

@Issue("VERTISTEST-1509")
@DisplayName("Отчет ЕГРН. Отчет")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PageEgrnReportTest {

    private static final String EGRN_LOGIN = "bro";
    private static final String EGRN_PASSWORD = "chakp1";
    private static final String EGRN_REPORT_PATH = "egrn-report/b025ef62da1442e9817f7c94228ad6f9/";
    private static final String OFFER_ID_PATH = "4269669184141158387/";
    private static final String ADDRESS_PATH = "Россия, Москва, Мытная улица, 7с1/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        passportSteps.login(EGRN_LOGIN, EGRN_PASSWORD);
        urlSteps.testing().path(EGRN_REPORT_PATH).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим переход из отчета в карточку оффера")
    public void shouldSeePathToOfferCard() {
        basePageSteps.onEgrnReportPage().block("Данные из\u00a0объявления").link("объявления").click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OFFER).path(OFFER_ID_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим переход из отчета в историю объявлений")
    public void shouldSeePathToHistory() {
        basePageSteps.onEgrnReportPage().block("Другие предложения").link("Смотреть историю объявлений").click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OTSENKA_KVARTIRY).path(ADDRESS_PATH)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим переход из отчета в карточку оффера")
    public void shouldSeePathToLinks() {
        basePageSteps.moveCursorAndClick(basePageSteps.onEgrnReportPage().block("Средняя цена").spanLink("Медианная"));
        basePageSteps.onEgrnReportPage().popupVisible()
                .should(hasText(containsString("Мы используем медианную цену.")));
    }
}
