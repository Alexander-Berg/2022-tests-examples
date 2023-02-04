package ru.auto.tests.desktop.element.forms;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface EvaluationResultSnippet extends VertisElement, WithButton {

    @Name("?")
    @FindBy(".//div[contains(@class, 'InfoPopup')] | " +
            ".//*[contains(@class, 'IconSvg_question')]")
    VertisElement helpIcon();

    @Name("Оценочная цена")
    @FindBy(".//div[@class = 'EvaluationResultSnippet__priceValue']")
    VertisElement evaluationPrice();
}