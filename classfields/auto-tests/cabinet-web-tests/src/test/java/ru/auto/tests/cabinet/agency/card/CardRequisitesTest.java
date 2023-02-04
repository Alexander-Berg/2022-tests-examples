package ru.auto.tests.cabinet.agency.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.SALON_INFO_LEGACY;
import static ru.auto.tests.desktop.mock.MockSalonInfo.salonInfoRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AGENCY_CABINET)
@DisplayName("О салоне")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CardRequisitesTest {

    private static int AGENCY_CLIENT_ID = 25718;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgencyClient"),
                stub("cabinet/AgencyAgencyGetClientId"),
                stub("cabinet/AgencyClientsGet"),
                stub("cabinet/ApiAccessClientAgency"),
                stub("cabinet/DealerAccountAgencyClient"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/AgencySalonInfoGet"),
                stub().withPostDeepEquals(SALON_INFO_LEGACY)
                        .withRequestBody(
                                salonInfoRequest().setClientId(AGENCY_CLIENT_ID).getBody())
                        .withStatusSuccessResponse()
        ).create();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Изменение данных на странице «О салоне»")
    public void shouldChangeDataLegal() {
        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CARD).addParam(CLIENT_ID, "25718").open();

        steps.onCabinetSalonCardPage().aboutBlock().input("Название", "SALON", 100);
        steps.onCabinetSalonCardPage().aboutBlock()
                .input("Сайт", "http://www.major-expert.ru", 100);
        steps.onCabinetSalonCardPage().input("Дополнительная информация", "Дополнительные сведения", 100);
        steps.onCabinetSalonCardPage().input("Расположение", "Москва", 100);
        steps.onCabinetSalonCardPage().input("Арендодатель", "Автору", 100);
        steps.onCabinetSalonCardPage().input("Контактное лицо", "Менеджер", 100);
        steps.onCabinetSalonCardPage().getPhone(0).input("countryCode", "7", 100);
        steps.onCabinetSalonCardPage().getPhone(0).input("cityCode", "495", 100);
        steps.onCabinetSalonCardPage().getPhone(0).input("phone", "9999999", 100);
        steps.onCabinetSalonCardPage().getPhone(0).input("extention", "1234", 100);
        steps.onCabinetSalonCardPage().getPhone(0).input("callFrom", "08", 100);
        steps.onCabinetSalonCardPage().getPhone(0).input("callTill", "20", 100);
        steps.onCabinetSalonCardPage().button("Сохранить изменения").click();

        steps.onCabinetSalonCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные успешно сохранены"));
    }
}
