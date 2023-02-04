package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 11.05.18
 */
public interface NoActiveOffersStub extends VertisElement {

    @Name("Ссылка «{{ name }}»")
    @FindBy(".//main//a[contains(., '{{ name }}')]")
    VertisElement link(@Param("name") String name);
}
