package ru.auto.tests.desktop.element.cabinet.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MarkBlock extends VertisElement {

    @Name("Удаление марки")
    @FindBy(".//div[@class = 'CardFileUploaderPreview__clear']")
    VertisElement deleteMarkButton();
}
