package ru.auto.tests.desktop.element.history;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Contacts extends VertisElement, WithButton {

    @Name("Марка, модель")
    @FindBy(".//div[contains(@class,'HistoryOfferContacts__mmm')]")
    VertisElement mmm();

    @Name("Цена")
    @FindBy(".//span[contains(@class, 'HistoryOfferContacts__price')]")
    VertisElement price();

    @Name("Продавец")
    @FindBy(".//div[contains(@class, 'sellerName')]")
    VertisElement seller();

    @Name("Адрес")
    @FindBy(".//span[contains(@class, 'sellerAddress')]")
    VertisElement address();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class, '__phone')]")
    VertisElement showPhoneButton();

    @Name("Список телефонов")
    @FindBy(".//div[contains(@class, 'card-phones__item')] | " +
            ".//div[contains(@class, 'CardPhone-module__phone-title')]")
    ElementsCollection<VertisElement> phonesList();

    @Name("Кнопка «Написать»")
    @FindBy(".//button[contains(@class, 'PersonalMessage')]")
    VertisElement sendMessageButton();

    @Name("Кнопка избранного")
    @FindBy(".//span[contains(@class, 'ButtonFavorite')]")
    VertisElement favoriteButton();

}
