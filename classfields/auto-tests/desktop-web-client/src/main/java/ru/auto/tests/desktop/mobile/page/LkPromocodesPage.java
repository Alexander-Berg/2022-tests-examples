package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface LkPromocodesPage extends BasePage {

    @Name("Сообщение об ошибке")
    @FindBy("//div[contains(@class, 'TextInput__placeholder')]")
    VertisElement errorMessage();
}
