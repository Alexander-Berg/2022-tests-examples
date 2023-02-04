package ru.yandex.realty.managementnew.tariff;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.TARIFFS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;

@Tag(JURICS)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница тарифа")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class TariffsPageTest {

    private static final String REAL_LINK = "https://realty.yandex.ru/export/prices/prices_agency_actual.pdf";
    private static final String INFORMER_TEXT = "Кроме республики Крым и города Севастополь";
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
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Прайс-лист тарифов»")
    public void shouldSeePriceListCLick() {
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().link("Прайс-лист тарифов").click();
        managementSteps.waitUntilSeeTabsCount(2);
        managementSteps.switchToNextTab();
        urlSteps.fromUri(REAL_LINK).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на информер")
    public void shouldSeeInformerClick() {
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().informer(INFORMER_TEXT).click();
        managementSteps.onTariffsPage().informerPopup().should(hasText(INFORMER_TEXT));
    }
}
