package ru.auto.tests.cabinet.feeds;

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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.DONT_DELETE_PHOTO_CHECKBOX;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.NOTIFY_SUCCESS;
import static ru.auto.tests.desktop.page.cabinet.CabinetFeedsPage.CARS_USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. Действия с готовым фидом")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FeedsActionsTest {

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
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/FeedsSettings").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Изменение фида")
    public void shouldChangeFeed() {
        mockRule.with("cabinet/FeedsSettingsCarsUsedIsActiveTrueLeaveAddedImagesFalsePost").update();

        steps.onCabinetFeedsPage().feed(CARS_USED).click();
        steps.onCabinetFeedsPage().feed(CARS_USED).checkboxChecked(DONT_DELETE_PHOTO_CHECKBOX).click();
        steps.onCabinetFeedsPage().feed(CARS_USED).button("Сохранить фид").click();
        steps.onCabinetFeedsPage().notifier().waitUntil(isDisplayed()).should(hasText(NOTIFY_SUCCESS));
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Удаление фида")
    public void shouldDeleteFeed() {
        mockRule.with("cabinet/FeedsSettingsCarsUsedDelete").update();

        steps.onCabinetFeedsPage().feed(CARS_USED).click();
        steps.onCabinetFeedsPage().feed(CARS_USED).button("Удалить фид").click();
        steps.onCabinetFeedsPage().popup().button("Хорошо").click();
        steps.onCabinetFeedsPage().notifier().waitUntil(isDisplayed()).should(hasText("Фид успешно удалён"));
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Отключение и подключение фида")
    public void shouldDisableAndEnableFeed() {
        mockRule.with("cabinet/FeedsSettingsCarsUsedIsActiveFalsePost",
                "cabinet/FeedsSettingsCarsUsedIsActiveTruePost").update();

        steps.onCabinetFeedsPage().feed(CARS_USED).click();
        steps.onCabinetFeedsPage().feed(CARS_USED).button("Отключить").click();
        steps.onCabinetFeedsPage().notifier().waitUntil(isDisplayed()).should(hasText(NOTIFY_SUCCESS));
        steps.onCabinetFeedsPage().notifier().waitUntil(not(isDisplayed()));
        steps.onCabinetFeedsPage().feed(CARS_USED).button("Подключить").should(isDisplayed()).click();
        steps.onCabinetFeedsPage().notifier().waitUntil(isDisplayed()).should(hasText(NOTIFY_SUCCESS));
        steps.onCabinetFeedsPage().feed(CARS_USED).button("Отключить").should(isDisplayed());
    }
}
