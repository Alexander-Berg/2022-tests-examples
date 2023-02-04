package ru.auto.tests.desktop.page.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.element.lk.CreditFilter;
import ru.auto.tests.desktop.element.lk.CreditsChooseCarPopup;
import ru.auto.tests.desktop.element.lk.CreditsClaimItem;
import ru.auto.tests.desktop.element.lk.CreditsForm;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkCreditsPage extends BasePage, WithButton {

    String SBERBANK_HOST = "online.sberbank.ru";

    String SEND_REQUEST = "Отправить заявку";

    @Name("Список заявок")
    @FindBy("//div[contains(@class, 'BankSnippetCar')] | " +
            "//div[contains(@class, 'CreditClaimSnippetBankDesktop MyCreditsClaimList__claim')]")
    ElementsCollection<CreditsClaimItem> creditsClaimsList();

    @Name("Форма заявки на кредит")
    @FindBy("//div[contains(@class, 'CreditApplicationForm')] | " +
            "//div[contains(@class, 'CreditForm')] | " +
            "//div[contains(@class, 'WizardDesktop')]")
    CreditsForm creditsForm();

    @Name("Поп-ап выбора машины")
    @FindBy("//div[contains(@class, 'CreditChooseCar')]")
    CreditsChooseCarPopup creditsChooseCarsPopup();

    @Name("Фильтр")
    @FindBy("//div[contains(@class, 'MyCreditsClaimList__productTypeSelector')]")
    CreditFilter filter();

    @Step("Получаем заявку с индексом {i}")
    default CreditsClaimItem getCreditClaim(int i) {
        return creditsClaimsList().should(hasSize(greaterThan(i))).get(i);
    }

}
