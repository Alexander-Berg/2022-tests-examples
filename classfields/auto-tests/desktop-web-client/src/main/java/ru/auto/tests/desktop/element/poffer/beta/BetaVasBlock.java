package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BetaVasBlock extends VertisElement {

    @Name("Блок с бесплатным размещением")
    @FindBy(".//div[contains(@class, 'VasFormUserSnippet_type_free')]")
    BetaFreeVasBlock free();

}