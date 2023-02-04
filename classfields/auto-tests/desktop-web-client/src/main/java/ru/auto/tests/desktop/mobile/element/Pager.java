package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Pager extends VertisElement, WithButton {

    @Name("Текущая страница")
    @FindBy(".//a[contains(@class, 'Button_disabled')]")
    VertisElement currentPage();

}
