package ru.auto.tests.desktop.mobile.element.filters;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.listing.MMMFilter;

public interface Mmm extends VertisElement, WithButton {

    @Name("Марка «{{ text }}»")
    @FindBy(".//span[.= '{{ text }}']")
    VertisElement mark(@Param("text") String text);

    @Name("Марка, модель, поколение")
    @FindBy(".//div[contains(@class, 'ListingFiltersPopup__mmm')]")
    MMMFilter mmm();

}
