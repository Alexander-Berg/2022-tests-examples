package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;

public interface BetaPtsBlock extends VertisElement, WithCheckbox, WithSelect {

    String YEAR = "Год";
    String MONTH = "Месяц";
    String ON_WARRANTY = "На гарантии";
    String YEAR_OF_END = "Год окончания";

    @Name("Тип ПТС «{{ ptsType }}»")
    @FindBy(".//button[.='{{ ptsType }}']")
    VertisElement ptsType(@Param("ptsType") String ptsType);

    @Name("Владельцев «{{ owners }}»")
    @FindBy(".//div[@class='OfferFormOwnersNumberField']//button[.='{{ owners }}']")
    VertisElement ownersCount(@Param("owners") String owners);

}
