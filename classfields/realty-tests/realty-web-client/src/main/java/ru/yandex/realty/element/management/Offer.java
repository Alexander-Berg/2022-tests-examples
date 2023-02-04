package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Offer extends AtlasWebElement {

    @Name("Адрес")
    @FindBy(".//td[contains(@class, 'manage-table__address-cell')]//a")
    AtlasWebElement address();

    @Name("Элемент цены")
    @FindBy(".//td[contains(@class, 'manage-table__price-cell')]")
    PriceElement price();

    @Name("Кнопка «Поднять»")
    @FindBy(".//button[contains(@class, 'manage-table__control_service_raising')]")
    AtlasWebElement payRaisingButton();

    @Name("Кнопка «Премиум»")
    @FindBy(".//button[contains(@class, 'manage-table__control_service_premium')]")
    AtlasWebElement payPremiumButton();

    @Name("Кнопка «Продвижение»")
    @FindBy(".//button[contains(@class, 'manage-table__control_service_promotion')]")
    AtlasWebElement payPromotionButton();

    @Name("Тумблер выключения оффера")
    @FindBy(".//span[contains(@class, 'manage-table__control_type_power')]//button")
    AtlasWebElement powerSwitch();

    @Name("Кнопка редактирования оффера")
    @FindBy(".//a[contains(@class, 'manage-table__control_type_edit')]")
    AtlasWebElement editButton();

    @Name("Кнопка удаления оффера")
    @FindBy(".//div[contains(@class, 'manage-table__control_type_remove')]")
    AtlasWebElement deleteButton();

    @Name("Кнопка обновления оффера")
    @FindBy(".//button[contains(@class, 'manage-table__control_type_extend')]")
    AtlasWebElement refreshButton();

}
