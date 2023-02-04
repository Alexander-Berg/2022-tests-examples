package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.element.management.PublicProfile;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

@Link("https://st.yandex-team.ru/VERTISTEST-1477")
@DisplayName("Публичный профиль")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PublicProfileTest {

    private static final String SYMBOLS_1002 = "Часто говорят, что модель общества, составными элементами которого " +
            "являются индивиды, заимствована из абстрактных юридических форм договора и обмена. С этой точки зрения " +
            "товарное общество представляется как договорное объединение отдельных юридических субъектов. Возможно, " +
            "это так. Во всяком случае, политическая теория XVII-XVIII столетий, видимо, часто следует этой схеме. " +
            "Но не надо забывать, что в ту же эпоху существовала техника конституирования индивидов как коррелятов " +
            "власти и знания. Несомненно, индивид есть вымышленный атом «идеологического» представления об обществе; " +
            "но он есть также реальность, созданная специфической технологией власти, которую я назвал «дисциплиной». " +
            "Надо раз и навсегда перестать описывать проявления власти в отрицательных терминах: она, мол, " +
            "«исключает», «подавляет», «цензурует», «извлекает», «маскирует», «скрывает». На самом деле, власть " +
            "производит. Она производит реальность; она производит области объектов и ритуалы истины. " +
            "Индивид и знание, которое можно...";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AccountType.AGENCY);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Профиль выключен -> видим «Включите профиль, чтобы он появился на сервисе»")
    public void shouldSeeEnableWarnMessage() {
        managementSteps.onManagementNewPage().settingsContent().publicProfile().tumblerButton()
                .should(not(isChecked()));
        managementSteps.onManagementNewPage().settingsContent().publicProfile()
                .message(PublicProfile.ENABLE_PROFILE_WARN).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Профиль включен -> не видим «Включите профиль, чтобы он появился на сервисе»")
    public void shouldNotSeeEnableWarnMessage() {
        managementSteps.onManagementNewPage().settingsContent().publicProfile().enableProfile();
        managementSteps.onManagementNewPage().settingsContent().publicProfile()
                .message(PublicProfile.ENABLE_PROFILE_WARN).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка «Соглашение об информационном сотрудничестве»")
    public void shouldSeeInfoLinkClick() {
        managementSteps.onManagementNewPage().settingsContent()
                .link("Соглашение об информационном сотрудничестве").click();
        managementSteps.switchToNextTab();
        urlSteps.fromUri("https://yandex.ru/legal/realty_agreement/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка «Соглашение об информационном сотрудничестве»")
    public void shouldSeeOverThousandSymbols() {
        managementSteps.onManagementNewPage().settingsContent().publicProfile().descriptionArea()
                .sendKeys(SYMBOLS_1002);
        managementSteps.onManagementNewPage().settingsContent().publicProfile().descriptionArea()
                .should(hasText(SYMBOLS_1002.substring(0, 1000)));
    }
}
