package ru.auto.tests.desktop.element.forms;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface DealerVas extends VertisElement, WithCheckbox {

    @Name("Услуга «{{ text }}»")
    @FindBy(".//label[contains(@class, 'VasFormDealerItem ') and .//div[contains(@class, 'VasFormDealerItem__name') and .= '{{ text }}']]")
    VertisElement vas(@Param("text") String text);
}