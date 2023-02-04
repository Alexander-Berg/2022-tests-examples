package ru.yandex.realty.mobile.element.listing;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface TouchVillage {

    @Name("Срок сдачи")
    @FindBy(".//span[contains(@class, 'deliveryDates')]")
    AtlasWebElement deliveryDates();

    @Name("Тайтл")
    @FindBy(".//div[contains(@class, 'title')]")
    AtlasWebElement title();

    @Name("Класс")
    @FindBy(".//span[contains(@class, 'villageClass')]")
    AtlasWebElement villageClass();

}
