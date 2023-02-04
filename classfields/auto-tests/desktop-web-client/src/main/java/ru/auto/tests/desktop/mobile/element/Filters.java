package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.element.listing.MMMFilter;

public interface Filters extends VertisElement, WithCheckbox {

    String MARK_AND_MODEL = "Марка и модель";
    String MARK_MODEL = "Марка, модель";

    @Name("Секция «{{ text }}»")
    @FindBy(".//button[./span[.='{{ text }}']] | " +
            ".//a[contains(@class, 'ListingAmpFilter__section') and .='{{ text }}']")
    VertisElement section(@Param("text") String Text);

    @Name("Марка, модель, поколение")
    @FindBy(".//div[contains(@class, 'MMMMultiFilter')]")
    MMMFilter mmm();

    @Name("Кнопка «Параметры»")
    @FindBy(".//a[contains(@class, 'MoreShortFilterButton') or contains(@class, 'MoreShortFilterButton')] | " +
            ".//button[contains(@class, 'MoreShortFilterButton')] | " +
            ".//div[contains(@class, 'ListingHead2__buttons')]/div | " +
            ".//span[contains(@class, 'Tag_type_filter') and .//span[.= 'Параметры']] | " +
            ".//a[contains(@class, 'ListingAmpFilter__allButton')]")
    VertisElement paramsButton();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}'] | " +
            ".//span[.= '{{ text }}'] |" +
            ".//div[contains(@class, 'PseudoInput ') and .= '{{ text }}']")
    FilterButton button(@Param("text") String text);

}
