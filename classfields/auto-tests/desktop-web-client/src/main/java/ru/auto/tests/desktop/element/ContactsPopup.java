package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithFavoritesButton;
import ru.auto.tests.desktop.element.card.DealerSleepBlock;
import ru.auto.tests.desktop.element.card.NoteBar;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ContactsPopup extends VertisElement, WithFavoritesButton {

    @Name("Продавец")
    @FindBy(".//a[contains(@class, '_sellerName')]")
    VertisElement seller();

    @Name("Время звонка")
    @FindBy(".//div[contains(@class, '__phoneSchedule')] | " +
            ".//div[contains(@class, '__phoneDescription')]")
    VertisElement time();

    @Name("Блок с телефонами")
    @FindBy(".//div[contains(@class, 'SellerPhonePopup__phoneList')] | .//div[contains(@class, 'SellerPopup__phonesList')]")
    VertisElement phones();

    @Name("Список телефонов")
    @FindBy(".//a[contains(@class, 'SellerPopup__phoneNumber')]")
    ElementsCollection<VertisElement> phonesList();

    @Name("Иконка закрытия поп-апа")
    @FindBy(".//*[contains(@class, 'modal__close')] | " +
            ".//div[contains(@class, 'ModalDialogCloser')] | " +
            "..//div[contains(@class, 'Modal__closer')]")
    VertisElement closer();

    @Name("Заметка")
    @FindBy(".//*[contains(@class, 'OfferNoteView_hasHover') or contains(@class, 'OfferNote_noteEdit')]")
    NoteBar noteBar();

    @Name("Кнопка «Доехать с Яндекс.Такси»")
    @FindBy(".//a[contains(@class, 'SellerPopupFooter__taxiButton')]")
    VertisElement taxiButton();

    @Name("Адрес")
    @FindBy(".//span[contains(@class, 'SellerPopupFooter__address')]")
    VertisElement address();

    @Name("Плашка «Сейчас дилер не работает»")
    @FindBy(".//div[contains(@class, 'DealerCallbackTile-module__DealerCallbackTile') " +
            "and not(contains(@class, 'DealerCallbackTile_hide'))]")
    DealerSleepBlock dealerSleepBlock();

    @Step("Получаем телефон с индексом {i}")
    default VertisElement getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Кнопка «Подписаться на объяавления»")
    @FindBy(".//div[contains(@class, 'SubscriptionSaveButton')]")
    VertisElement subscribeButton();
}
