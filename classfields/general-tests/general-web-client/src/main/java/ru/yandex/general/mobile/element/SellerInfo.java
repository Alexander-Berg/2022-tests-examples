package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SellerInfo extends VertisElement, Link, Image {

    @Name("Список активных полосок индикатора скора")
    @FindBy(".//div[contains(@class, 'ScoreIndicator__active')]")
    ElementsCollection<VertisElement> activeScoreIndicatorList();

    @Name("Имя")
    @FindBy(".//span[contains(@class, '_h3Medium')]")
    VertisElement name();

    @Name("Кол-во офферов")
    @FindBy(".//span[contains(@class, '_offersCount')]")
    VertisElement offersCount();

}
