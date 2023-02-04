package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Input;

public interface ListingFilter extends Input {

    @Name("Закрыть фильтр")
    @FindBy(".//div[contains(@class,'Chip__iconClose')]")
    VertisElement closeFilter();
}
