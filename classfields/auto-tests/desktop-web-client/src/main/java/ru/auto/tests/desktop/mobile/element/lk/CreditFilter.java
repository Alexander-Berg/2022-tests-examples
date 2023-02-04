package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.page.BasePage;

public interface CreditFilter extends BasePage, WithButton {

    String FILTER_CASH = "Наличными";
    String FILTER_REFIN = "Рефинансирование";
    String FILTER_ALL = "Все";

    @Name("Нажатая кнопка «{{ text }}» фильтра")
    @FindBy(".//button[contains(@class, 'Button_checked') and . = '{{ text }}']")
    VertisElement buttonChecked(@Param("text") String text);
}
