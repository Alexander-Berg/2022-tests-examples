package ru.auto.tests.desktop.element.cabinet.manager;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.page.BasePage;

public interface CreditSection extends VertisElement, BasePage, CreditSectionElement {

    @Name("Секция {{ text }}»")
    @FindBy("//div[@class='ApplicationCredit__section' and contains(., '{{ text }}')]")
    CreditSectionElement creditSection(@Param("text") String text);
}
