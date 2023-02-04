package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FromWebToAppSplash extends VertisElement {

    String SPLASH_EDIT_OFFER_TEXT = "Отредактируйте объявление в пару касаний на Авто.ру\nОтредактировать в приложении";

    @Name("Кнопка закрытия")
    @FindBy(".//a[contains(@class, '_close')]")
    VertisElement closeButton();

    @Name("Кнопка действия")
    @FindBy(".//a[contains(@class, '_button')]")
    VertisElement button();

}
