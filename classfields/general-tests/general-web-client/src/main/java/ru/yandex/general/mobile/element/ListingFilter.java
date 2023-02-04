package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ListingFilter extends Input {

    @Name("Крестик «Закрыть»")
    @FindBy(".//div[contains(@class,'Chip__iconClose')]")
    VertisElement closeFilter();
}
