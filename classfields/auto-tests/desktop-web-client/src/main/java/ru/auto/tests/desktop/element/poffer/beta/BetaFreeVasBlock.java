package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BetaFreeVasBlock extends VertisElement {

    String FREE_SUBMIT_TEXT = "Разместить бесплатно на 60 дней";

    @Name("Кнопка «Разместить»")
    @FindBy("./button[contains(@class, 'VasFormUserSnippet__button')]")
    VertisElement submitButton();

}