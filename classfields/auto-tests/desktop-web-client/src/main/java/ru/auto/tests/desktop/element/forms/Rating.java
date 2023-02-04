package ru.auto.tests.desktop.element.forms;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface Rating extends VertisElement, WithCheckbox {

    @Name("Звезда «{{ index }}»")
    @FindBy(".//span[@class = 'Rating__item' and @data-index = '{{ index }}']")
    VertisElement star(@Param("index") String index);

    @Name("Список закрашенных звезд")
    @FindBy(".//*[contains(@class, 'IconSvg_star-filled')]")
    ElementsCollection<VertisElement> filledStarsList();
}