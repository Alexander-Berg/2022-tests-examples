package ru.auto.tests.cabinet.header;

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
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static java.lang.String.valueOf;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class BillTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerLoyaltyReport",
                "cabinet/DealerInfoMultipostingDisabled",
                "cabinet/ClientsGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Открытие поп-апа оплаты")
    public void shouldPopupBalance() {
        steps.onCabinetOffersPage().header().financialWidget().click();
        steps.onCabinetOffersPage().header().financialWidget().rechargeAmount().click();
        steps.onCabinetOffersPage().header().financialWidget().rechargeAmount().sendKeys(valueOf(getRandomShortInt()));
        steps.onCabinetOffersPage().header().financialWidget().button("Пополнить").click();
        steps.onCabinetDashboardPage().popupBillingBlock().isEnabled();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Поп-ап «Программа лояльности»")
    public void shouldSeeLoyaltyProgram() {
        steps.moveCursor(steps.onCabinetOffersPage().header().loyalty());
        steps.onCabinetOffersPage().header().loyaltyPopup().waitUntil(isDisplayed())
                .should(hasText("Поздравляем, у Вас статус проверенного дилера!\nЧто это даёт:\n" +
                        "Бонусы в 5% на Ваш счёт в Авто.ру. Потратьте его на дополнительные услуги.\n" +
                        "Специальный знак «Проверенный дилер» у всех Ваших объявлений.\n" +
                        "Вы выполнили следующие условия:\nКарточка салона не была заморожена модерацией.\n" +
                        "Вы не продаете автомобили со скрученным пробегом.\n" +
                        "Вы указали актуальные данные в карточке дилера.\n" +
                        "В последние 3 месяца Вы прошли нашу выездную проверку.\n" +
                        "Подробно о проверенном дилере\nСкидка по программе лояльности\n" +
                        "Бонусы программы лояльности за текущий месяц\n12 677 ₽"));
    }
}
