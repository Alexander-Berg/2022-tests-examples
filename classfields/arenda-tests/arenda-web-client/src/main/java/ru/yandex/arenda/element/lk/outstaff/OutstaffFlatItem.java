package ru.yandex.arenda.element.lk.outstaff;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface OutstaffFlatItem extends AtlasWebElement {

    @Name("Ссылка заголовка")
    @FindBy(".//a[contains(@class,'OutstaffSearchFlatsItem__title')]")
    AtlasWebElement link();

    @Name("Скелетон")
    @FindBy(".//div[contains(@class,'Skeleton__item')]")
    AtlasWebElement skeletonItem();
}
