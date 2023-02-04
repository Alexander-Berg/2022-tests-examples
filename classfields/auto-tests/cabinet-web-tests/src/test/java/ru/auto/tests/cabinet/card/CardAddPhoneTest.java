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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Добавление второго телефона в «Информация о салоне»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CardAddPhoneTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/SearchCarsBreadcrumbs",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DesktopSalonInfoGet",
                "cabinet/DesktopSalonInfoUpdatePhone").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Добавление второго телефона")
    public void shouldAddNumber() {
        steps.onCabinetSalonCardPage().getPhone(0).button("+").click();
        steps.onCabinetSalonCardPage().getPhone(1).input("countryCode", "8", 100);
        steps.onCabinetSalonCardPage().getPhone(1).input("cityCode", "495", 100);
        steps.onCabinetSalonCardPage().getPhone(1).input("phone", "0010398", 100);
        steps.onCabinetSalonCardPage().getPhone(1).input("extention", "1234", 100);
        steps.onCabinetSalonCardPage().getPhone(1).input("callFrom", "08", 100);
        steps.onCabinetSalonCardPage().getPhone(1).input("callTill", "20", 100);
        steps.onCabinetSalonCardPage().button("Сохранить изменения").click();
        steps.onCabinetSalonCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные успешно сохранены"));
    }
}
