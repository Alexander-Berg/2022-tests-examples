package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Complectation extends VertisElement {

    @Name("Ссылка на модификацию")
    @FindBy("./following-sibling::tr//a")
    VertisElement modificationUrl();
}
