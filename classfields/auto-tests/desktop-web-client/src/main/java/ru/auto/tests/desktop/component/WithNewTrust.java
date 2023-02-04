package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.NewTrust;

public interface WithNewTrust extends WithCheckbox{

    @Name("Фрейм нового траста")
    @FindBy("//iframe[contains(@class, 'YpcDieHardFrame')]")
    VertisElement newTrustFrame();

    @Name("Тело фрейма нового траста")
    @FindBy("//html")
    NewTrust newTrust();

}
