package ru.auto.tests.cabinet.settings;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CHATS;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockDealerSettings.mockDealerSettings;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_SETTINGS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Настройки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class SettingsChatsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/ChatAggregatorDelete"),

                stub().withPutDeepEquals(DEALER_SETTINGS)
                        .withRequestBody(mockDealerSettings().setChatEnabledRequest(true).getRequestBody())
                        .withStatusSuccessResponse(),

                stub().withPutDeepEquals(DEALER_SETTINGS)
                        .withRequestBody(mockDealerSettings().setChatEnabledRequest(false).getRequestBody())
                        .withStatusSuccessResponse()
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).path(CHATS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @Issue("AUTORUFRONT-21536")
    @DisplayName("Включение и выключение чатов")
    public void shouldEnableAndDisableChats() {
        steps.onSettingsChatsPage().section("Подключите чаты").inactiveToggle("Подключить чаты").click();
        steps.onSettingsChatsPage().notifier().should(isDisplayed()).should(hasText("Данные сохранены"));
        steps.onSettingsChatsPage().notifier().waitUntil(not(isDisplayed()));
        steps.onSettingsChatsPage().section("Подключите чаты").activeToggle("Подключить чаты")
                .waitUntil(isDisplayed());

        steps.onSettingsChatsPage().section("Агрегатор чатов").inactiveToggle("Бачата").click();
        steps.onSettingsChatsPage().notifier().waitUntil(not(isDisplayed()));
        steps.onSettingsChatsPage().section("Агрегатор чатов").activeToggle("Бачата").waitUntil(isDisplayed())
                .click();
        steps.onSettingsChatsPage().section("Агрегатор чатов").inactiveToggle("Бачата").waitUntil(isDisplayed());

        steps.onSettingsChatsPage().section("Подключите чаты").activeToggle("Подключить чаты").click();
        steps.onSettingsChatsPage().notifier().should(isDisplayed()).should(hasText("Данные сохранены"));
        steps.onSettingsChatsPage().section("Подключите чаты").inactiveToggle("Подключить чаты")
                .waitUntil(isDisplayed());
        steps.onSettingsChatsPage().section("Агрегатор чатов").waitUntil(not(isDisplayed()));
    }
}
