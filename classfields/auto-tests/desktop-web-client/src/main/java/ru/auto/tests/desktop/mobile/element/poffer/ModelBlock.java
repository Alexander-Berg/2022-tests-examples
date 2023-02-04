package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface ModelBlock extends VertisElement, WithButton, WithInput {

    @Name("Список моделей")
    @FindBy(".//div[@class = 'ModelFieldListItem']")
    ElementsCollection<VertisElement> modelsList();

    @Name("Модель «{{ text }}» в списке")
    @FindBy(".//div[@class = 'ModelFieldListItem' and . = '{{ text }}']")
    VertisElement model(@Param("text") String Text);

}
