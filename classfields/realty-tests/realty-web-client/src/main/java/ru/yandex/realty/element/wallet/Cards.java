package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Cards extends Button {

    String ADD_CARD = "Добавить";

    @Name("Тайтл")
    @FindBy(".//div[contains(@class,'WalletSection__title')]")
    AtlasWebElement title();

    @Name("Удалить карту")
    @FindBy(".//button[@data-test='wallet-card-delete']")
    AtlasWebElement deleteCardButton();

    @Name("Привязанная карта VISA")
    @FindBy(".//div[contains(@data-test, 'wallet-card-visa')]")
    AtlasWebElement cardTypeVISA();

    @Name("Привязанная карта MASTER CARD")
    @FindBy(".//div[contains(@data-test, 'wallet-card-mastercard')]")
    AtlasWebElement cardTypeMasterCard();

    @Name("Кнопка саджеста карт")
    @FindBy(".//button[@data-test='wallet-cards-dropdown-trigger']")
    AtlasWebElement suggestButton();

    @Name("Чекбокс «Основная карта»")
    @FindBy(".//label[contains(@class,'WalletCards__preferredCard')]")
    AtlasWebElement mainCardCheckbox();

}
