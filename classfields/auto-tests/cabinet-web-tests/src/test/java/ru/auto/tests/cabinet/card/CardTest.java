package ru.auto.tests.cabinet.card;

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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.SALON_INFO_LEGACY;
import static ru.auto.tests.desktop.mock.MockSalonInfo.salonInfoRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Информация о салоне")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CardTest {

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
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/DesktopSalonInfoGet"),
                stub().withPostDeepEquals(SALON_INFO_LEGACY)
                        .withRequestBody(
                                salonInfoRequest().getBody())
                        .withStatusSuccessResponse()
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Изменение информации о салоне")
    public void shouldChangeCard() {
        steps.onCabinetSalonCardPage().input("Название", "SALON", 100);
        steps.onCabinetSalonCardPage().input("Сайт", "http://www.major-expert.ru", 100);
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

        steps.onCabinetFeedsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Данные успешно сохранены"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение значка модерации")
    public void shouldSeeModerationIcon() {
        mockRule.overwriteStub(6, stub("cabinet/DesktopSalonInfoGetUpdate"));

        urlSteps.refresh();

        steps.onCabinetWalletPage().moderationIcon().should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Выбор марки из списка")
    public void shouldSelectMark() {
        steps.onCabinetSalonCardPage().selectItem("Выбрать марку", "AMC");
        steps.onCabinetSalonCardPage().selectPopup().waitUntil(not(isDisplayed()));
        steps.onCabinetSalonCardPage().select("AMC").waitUntil(isDisplayed());
    }
}
