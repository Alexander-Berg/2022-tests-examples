package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface BetaMarkBlock extends VertisElement, WithInput, WithButton {

    String MARK = "Марка";
    String ALL_MARKS = "Все марки";

    @Name("Список марок")
    @FindBy("//li[@class='MarkFieldTile']")
    ElementsCollection<VertisElement> marksList();

    @Name("Марка «{{ text }}» в списке")
    @FindBy(".//li[@class='MarkFieldTile' and .= '{{ text }}']")
    VertisElement mark(@Param("text") String Text);
}
