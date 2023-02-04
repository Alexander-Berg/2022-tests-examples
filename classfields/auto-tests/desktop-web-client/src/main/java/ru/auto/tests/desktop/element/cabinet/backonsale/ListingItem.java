package ru.auto.tests.desktop.element.cabinet.backonsale;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface ListingItem extends VertisElement, WithButton {

    @Name("Изображение")
    @FindBy(".//a[@class = 'Link BackOnSaleItem__photo']")
    VertisElement image();

    @Name("Информация")
    @FindBy(".//div[contains(@class, 'Item__info')]")
    VertisElement info();

    @Name("Дополнительная информация")
    @FindBy(".//ul[contains(@class, 'Info__characters')]")
    VertisElement additionalInfo();

    @Name("Бейдж «Продан»")
    @FindBy(".//div[@class = 'BackOnSaleItem__soldBadge']")
    VertisElement soldBadge();

    @Name("Заголовок объявления")
    @FindBy(".//a[@class = 'Link SaleSpec__title']")
    VertisElement title();

    @Name("Цена автомобиля")
    @FindBy(".//div[contains(@class, 'Info__price')]")
    VertisElement price();

    @Name("Телефон")
    @FindBy(".//div[@class = 'BackOnSaleItem__phones']/button | " +
            ".//div[@class = 'BackOnSaleItem__phones']/div[@class = 'BackOnSaleItem__phone']")
    VertisElement phone();

    @Name("Основная информация об автомобиле")
    @FindBy(".//table[contains(@class, 'SaleSpec__table')]")
    VertisElement carInfo();

    @Name("Информация об объявлении")
    @FindBy(".//ul[@class = 'BackOnSaleItem__characters']")
    VertisElement offerInfo();

    @Name("Информация о дате и месте размещения объявления")
    @FindBy(".//div[@class = 'BackOnSaleItem__placement']")
    VertisElement datePlaceInfo();
}
