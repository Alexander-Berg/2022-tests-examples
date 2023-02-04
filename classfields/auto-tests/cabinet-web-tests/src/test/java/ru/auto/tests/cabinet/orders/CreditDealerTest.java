package ru.auto.tests.cabinet.orders;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CREDIT;
import static ru.auto.tests.desktop.consts.Pages.ORDERS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Заявки на кредит под дилером")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CreditDealerTest {

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
                "cabinet/ApiAccessClient").post();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Статус - Отключено")
    public void shouldSeeInactiveStatus() {
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(ORDERS).path(CREDIT).open();

        steps.onCabinetOrdersCreditPage().creditBlock().should(hasText("Получайте заявки на покупку авто в кредит\n" +
                "Вы указываете параметры кредитного калькулятора, который будет показан на объявлениях салона.\n" +
                "Клиенты выбирают подходящие условия кредитования и оставляют заявку. " +
                "Заявка приходит напрямую в дилерский центр, минуя банки и других контрагентов.\n" +
                "Для подключения обращайтесь по телефонам\n+7 495 755-55-77\n8 800 234-28-86"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Статус -  Подключено")
    public void shouldSeeActiveStatus() {
        mockRule.with("cabinet/ProductsActiveStatusActive").update();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(ORDERS).path(CREDIT).open();

        steps.onCabinetOrdersCreditPage().creditBlock().should(hasText("Получайте заявки на покупку авто в кредит\n" +
                "Вы указываете параметры кредитного калькулятора, который будет показан на объявлениях салона.\n" +
                "Клиенты выбирают подходящие условия кредитования и оставляют заявку. Заявка приходит напрямую " +
                "в дилерский центр, минуя банки и других контрагентов.\nДля подключения обращайтесь по телефонам" +
                "\n+7 495 755-55-77\n8 800 234-28-86"));
    }
}
