package ru.auto.tests.mobile.deal;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Безопасная сделка. Форма. Блок «Средства для оплаты»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealFormBuyerBankDetailTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/SafeDealDealGetWithBuyerBankDetails",
                "desktop/SafeDealDealUpdateOfferPrice").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть банковские реквизиты под покупателем")
    public void shouldSeeBankDetailsForPaymentByBuyer() {
        basePageSteps.onDealPage().section("Средства для оплаты").should(hasText("Средства для оплаты\nШаг 2 из 4." +
                "\n Подтверждение стоимости\nУкажите стоимость автомобиля, о которой вы договорились с продавцом" +
                "\nСтоимость автомобиля\n1 000 000 ₽\nКомиссия Авто.ру\n1 ₽\nИтого\n1 000 001 ₽\nРедактировать" +
                "\nПереведите 1 000 001 ₽ на счёт Авто.ру по QR-коду\n1. Сохраните QR-код или сделайте снимок экрана" +
                "\n2. В приложении вашего банка найдите оплату по QR-коду\n3. Загрузите изображение, проверьте " +
                "реквизиты и оплатите\nПереводите деньги только со своего счета физического лица.\nИначе они не будут " +
                "зачислены.\nПоказать реквизиты\nВнесенные средства\n0 ₽\nЗачисление средств может занять некоторое " +
                "время. Как только деньги поступят на специальный счет, мы сразу вам об этом сообщим"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть QR-код")
    public void shouldSeeQRCodeDownloadLink() {
        basePageSteps.onDealPage().qrCode().waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик на кнопку «Показать реквизиты» под покупателем")
    public void shouldClickShowBankDetailsButtonByBuyer() {
        basePageSteps.onDealPage().section("Средства для оплаты").button("Показать реквизиты").click();
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(0)
                .should(hasText("Наименование банка\nАО \"Тинькофф Банк\""));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(1)
                .should(hasText("ИНН получателя\n1111111111"));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(2)
                .should(hasText("Получатель\nООО Яндекс.Вертикали"));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(3)
                .should(hasText("Номер счета получателя\n40817810938160925982"));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(4)
                .should(hasText("БИК банка получателя\n044525225"));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(5)
                .should(hasText("ФИО плательщика\nИванов Иван Аркадиевич"));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(6)
                .should(hasText("Назначение платежа\nОплата стоимости автомобиля по договору купли-продажи №БС11187. " +
                        "НДС не облагается"));
        basePageSteps.onDealPage().section("Средства для оплаты").getRequisite(7)
                .should(hasText("Сумма платежа\n1 000 001 ₽"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик на кнопку «Скрыть реквизиты» под покупателем")
    public void shouldClickHideBankDetailsButtonByBuyer() {
        basePageSteps.onDealPage().section("Средства для оплаты").button("Показать реквизиты").click();
        basePageSteps.onDealPage().section("Средства для оплаты").button("Скрыть реквизиты")
                .waitUntil(isDisplayed()).click();
        basePageSteps.onDealPage().section("Средства для оплаты").requisitesList().waitUntil(not(isDisplayed()));
    }
}
