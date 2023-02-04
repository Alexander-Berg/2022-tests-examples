package ru.auto.tests.desktop.page.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithBillingModalPopup;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.element.lk.WalletBalanceBlock;
import ru.auto.tests.desktop.element.lk.WalletPayment;
import ru.auto.tests.desktop.element.lk.WalletTiedCardsBlock;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface WalletPage extends BasePage, WithPager, WithBillingModalPopup {

    @Name("Кошелёк")
    @FindBy("//div[@class = 'MyWallet']")
    VertisElement wallet();

    @Name("Блок «Баланс кошелька»")
    @FindBy("//div[@class = 'MyWallet']")
    WalletBalanceBlock walletBalance();

    @Name("Блок «Привязанные карты»")
    @FindBy("//div[contains(@class, 'MyWalletCards ')]")
    WalletTiedCardsBlock tiedCards();

    @Name("Список платежей")
    @FindBy(".//div[contains(@class, 'MyWalletHistory__tableRow')]")
    io.qameta.atlas.webdriver.ElementsCollection<WalletPayment> paymentsList();

    @Step("Получаем платёж с индексом {i}")
    default WalletPayment getPayment(int i) {
        return paymentsList().should(hasSize(greaterThan(i))).get(i);
    }
}
