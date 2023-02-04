package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface AllMarksPopup extends VertisElement, WithInput, WithButton {

    @Name("Список марок")
    @FindBy("//span[contains(@class, 'MarkFieldModal__item')]")
    ElementsCollection<VertisElement> marksList();
}
