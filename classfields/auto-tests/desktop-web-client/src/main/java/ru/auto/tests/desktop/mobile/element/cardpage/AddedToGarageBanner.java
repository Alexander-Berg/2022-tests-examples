package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface AddedToGarageBanner extends VertisElement, WithButton {

    String TO_GARAGE = "В гараж";

    @Name("Иконка закрытия")
    @FindBy(".//div[contains(@class, '_close')]")
    VertisElement close();

}
