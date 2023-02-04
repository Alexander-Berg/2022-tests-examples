package ru.auto.tests.desktop.element.catalog.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BodyItem extends VertisElement {

    @Name("Ссылка на новые объявления")
    @FindBy(".//a[contains(text(), 'нов')]")
    VertisElement newUrl();

    @Name("Ссылка на объявления с пробегом")
    @FindBy(".//a[contains(text(), 'с пробегом')]")
    VertisElement usedUrl();
}
