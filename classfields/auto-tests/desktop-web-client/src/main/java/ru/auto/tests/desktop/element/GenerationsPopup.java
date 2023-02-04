package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GenerationsPopup extends Select {

    @Name("Поколение «{{ text }}»")
    @FindBy(".//div[@class = 'PopupGenerationItem__name'][.= '{{ text }}']")
    VertisElement generationItem(@Param("text") String text);

    @Name("Кнопка сброса")
    @FindBy(".//div[contains(@class, 'PopupGenerationsListClear')]")
    VertisElement resetButton();
}
