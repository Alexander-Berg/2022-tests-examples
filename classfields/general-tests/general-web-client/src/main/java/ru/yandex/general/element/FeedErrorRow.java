package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FeedErrorRow extends VertisElement, Link {

    @Name("ID объявления")
    @FindBy(".//div[contains(@class, 'Table__id')]")
    VertisElement id();

    @Name("Ячейка «{{ value }}»")
    @FindBy(".//div[contains(@class, 'Table__cell')][{{ value }}]//*[contains(@class, 'Text')]")
    Link cell(@Param("value") int value);

    @Name("Ячейка фатальной ошибки «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FatalErrorsList__cell')][{{ value }}]/span")
    VertisElement fatalErrorCell(@Param("value") int value);

}
