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
public class SettingsWhiteListAddThreePhonesTest {

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
                "cabinet/DealerPhonesWhitelistGet",
                "cabinet/DealerPhonesWhitelistAvailable",
                "cabinet/DealerPhonesWhitelistAddThreePhones").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).path(WHITELIST).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Добавляем три телефона сразу")
    public void shouldAddThreePhones() {
        mockRule.overwriteStub(3, "cabinet/DealerPhonesWhitelistGetFourPhones");

        steps.onSettingsWhiteListPage().button("Добавить номера").click();
        steps.onSettingsWhiteListPage().popup()
                .input("Введите номер телефона или несколько в свободном формате", "+71110001112\n+71110001113\n+71110001114");
        steps.onSettingsWhiteListPage().popup().button("Добавить").click();
        steps.onSettingsWhiteListPage().notifier().should(isDisplayed()).should(hasText("3 номера добавлены"));
        steps.onSettingsWhiteListPage().phones().should(hasSize(4));
    }
}
