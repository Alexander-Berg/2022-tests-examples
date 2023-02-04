package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface NotFoundPage extends BasePage {

    @Name("Содержимое страницы")
    @FindBy("//div[@class='error']")
    VertisElement content();

}
