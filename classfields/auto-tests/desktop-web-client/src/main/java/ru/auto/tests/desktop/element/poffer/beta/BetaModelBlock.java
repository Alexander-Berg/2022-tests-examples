package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface BetaModelBlock extends VertisElement, WithInput, WithButton {

    String MODEL = "Модель";
    String ALL_MODELS = "Все модели";

    @Name("Список моделей")
    @FindBy("//span[contains(@class, 'ModelField__modelsListItem')] | " +
            "//span[contains(@class, 'OfferFormModelField__item')]")
    ElementsCollection<VertisElement> modelsList();

    @Name("Модель «{{ text }}» в списке")
    @FindBy(".//span[contains(@class, 'ModelField__modelsListItem') and . = '{{ text }}']")
    VertisElement model(@Param("text") String Text);
}
