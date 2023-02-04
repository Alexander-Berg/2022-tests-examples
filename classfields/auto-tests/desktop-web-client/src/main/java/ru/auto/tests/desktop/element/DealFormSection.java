package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DealFormSection extends VertisElement, WithButton, WithInput {

    @Name("Проверка шага")
    @FindBy(".//div[contains(@class, 'DealStepCheck')]")
    VertisElement stepCheck();

    @Name("Описание ожидания")
    @FindBy(".//div[contains(@class, 'DealNotification')]")
    VertisElement stepWaitingDescription();

    @Name("Цена, комиссия авто.ру")
    @FindBy(".//div[contains(@class, 'DealPaymentAmountForm__item')][1]")
    VertisElement paymentAmountTax();

    @Name("Цена, итого")
    @FindBy(".//div[contains(@class, 'DealPaymentAmountForm__item')][2]")
    VertisElement paymentAmountTotal();

    @Name("Фото ДКП, страница 1")
    @FindBy(".//div[@class='DkpPhotosForm__item'][1]")
    DealFormDkpPhotoItem dkpFirstPagePhoto();

    @Name("Фото ДКП, страница 2")
    @FindBy(".//div[@class='DkpPhotosForm__item'][2]")
    DealFormDkpPhotoItem dkpSecondPagePhoto();

    @Name("Список реквизитов")
    @FindBy(".//div[@class = 'DealBankDetailsItem']")
    ElementsCollection<VertisElement> requisitesList();

    @Step("Получаем реквизиты с индексом {i}")
    default VertisElement getRequisite(int i) {
        return requisitesList().should(hasSize(greaterThan(i))).get(i);
    }
}
