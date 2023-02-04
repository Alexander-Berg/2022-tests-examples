package ru.auto.tests.desktop.element.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CardContacts extends VertisElement {

    @Name("Имя продавца")
    @FindBy(".//a[contains(@class, 'CardSellerNamePlace__name')]")
    VertisElement sellerName();

    @Name("Ссылка «... в наличии»")
    @FindBy(".//a[contains(@class, 'CardSellerNamePlace__count')]")
    VertisElement inStockUrl();

    @Name("Место осмотра")
    @FindBy(".//div[contains(@class,'sale-location')]/span | " +
            ".//span[contains(@class, 'MetroListPlace_showAddress')] | " +
            ".//span[contains(@class, 'MetroListPlace ')]")
    VertisElement location();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//div[contains(@class,'show-phone_js_inited')] | " +
            ".//div[contains(@class, 'CardPhone-module__phone')] | " +
            ".//button[contains(@class, '__phone')]")
    VertisElement showPhoneButton();

    @Name("Список телефонов")
    @FindBy(".//div[contains(@class, 'card-phones__item')] | " +
            ".//div[contains(@class, 'CardPhone-module__phone-title')]")
    ElementsCollection<VertisElement> phonesList();

    @Name("Ссылка «Почему не мой номер»")
    @FindBy(".//span[contains(@class, 'OfferPhone__notMyNumberLink')]")
    VertisElement phonesIsVirtualDescription();

    @Name("Кнопка «Написать»")
    @FindBy(".//a[contains(@class,'personal-message__button')] | " +
            ".//div[contains(@class, 'PersonalMessage')]")
    VertisElement sendMessageButton();

    @Name("Кнопка «Обратный звонок»")
    @FindBy(".//div[contains(@class, 'CardCallbackButton')] | " +
            ".//button[contains(@class, 'CardCallbackButton__button')]")
    VertisElement callBackButton();

    @Name("Иконка «Проверенный дилер»")
    @FindBy(".//*[contains(@class, 'IconSvg_verified-dealer IconSvg_size_48')]")
    VertisElement loyaltyIcon();

    @Name("Кол-во офферов продавца")
    @FindBy("//div[@class = 'CardOwner']//a[contains(@class, 'CardSellerNamePlace__count')]")
    VertisElement offersCount();

}
