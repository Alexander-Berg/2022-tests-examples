package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface MarkBlock extends VertisElement, WithButton, WithInput {

    @Name("Список марок")
    @FindBy("//ul[@class='MarkField__items']")
    ElementsCollection<VertisElement> marksList();

    @Name("Марка «{{ text }}» в списке")
    @FindBy(".//li[@class='MarkFieldListItem' and .= '{{ text }}']")
    VertisElement mark(@Param("text") String text);

}
