package ru.auto.tests.cabinet.settings;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WHITELIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Настройки. Номера для выкупа")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SettingsWhiteListDeleteTwoPhonesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerPhonesWhitelistGetFourPhones",
                "cabinet/DealerPhonesWhitelistAvailable",
                "cabinet/DealerPhonesWhitelistDeleteTwoPhone").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).path(WHITELIST).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Удаляем два телефона из четырех")
    public void shouldDeleteTwoPhones() {
        mockRule.overwriteStub(3, "cabinet/DealerPhonesWhitelistGetTwoPhones");

        steps.onSettingsWhiteListPage().getPhone(0).checkbox().click();
        steps.onSettingsWhiteListPage().getPhone(1).checkbox().click();
        steps.onSettingsWhiteListPage().controls().button("Удалить").should(isDisplayed()).click();
        steps.onSettingsWhiteListPage().popup().should(isDisplayed()).should(hasText("Вы действительно хотите удалить " +
                "номера\n+7 111 000-11-14\n+7 111 000-11-13\nДа\nНет"));
        steps.onSettingsWhiteListPage().popup().button("Да").click();
        steps.onSettingsWhiteListPage().phones().should(hasSize(2));
    }
}
