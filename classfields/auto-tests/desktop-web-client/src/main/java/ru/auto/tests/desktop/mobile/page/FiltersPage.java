package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * Created by kopitsa on 14.09.17.
 */
public interface FiltersPage extends BasePage {

    @Name("Селектор «{{ filterName }}»")
    @FindBy("//select[./../div[. = '{{ filterName }}']]")
    VertisElement selector(@Param("filterName") String filterName);

    @Name("Кнопка применения фильтров")
    @FindBy("//div[@class = 'filters__submit-button']//button")
    VertisElement applyFiltersButton();

    @Name("Селектор от «{{ filterName }}»")
    @FindBy("(//div[.//div[contains(text(), '{{ filterName }}')] and contains(@class, 'FilterElements__row')]//select)[1] | " +
            "(//div[.//div[contains(text(), '{{ filterName }}')] and contains(@class, 'FiltersCarsPublicApi__row')]//select)[1]")
    VertisElement selectorFrom(@Param("filterName") String filterName);

    @Name("Селектор до «{{ filterName }}»")
    @FindBy("(//div[.//div[contains(text(), '{{ filterName }}')] and contains(@class, 'FilterElements__row')]//select)[2] | " +
            "(//div[.//div[contains(text(), '{{ filterName }}')] and contains(@class, 'FiltersCarsPublicApi__row')]//select)[2]")
    VertisElement selectorTo(@Param("filterName") String filterName);

    @Name("Инпут от «{{ filterName }}»")
    @FindBy("(//div[.//div[contains(., '{{ filterName }}')] and contains(@class, 'FilterElements__row')]//input)[1] | " +
            "(//div[.//div[contains(., '{{ filterName }}')] and contains(@class, 'ListingFiltersCarsPublicApi__row')]//input)[1]")
    VertisElement inputFrom(@Param("filterName") String filterName);

    @Name("Инпут до «{{ filterName }}»")
    @FindBy("(//div[.//div[contains(., '{{ filterName }}')] and contains(@class, 'FilterElements__row')]//input)[2] | " +
            "(//div[.//div[contains(., '{{ filterName }}')] and contains(@class, 'FiltersCarsPublicApi__row')]//input)[2]")
    VertisElement inputTo(@Param("filterName") String filterName);

    @Name("Инпут «{{ filterName }}»")
    @FindBy("//div[contains(., '{{ filterName }}')]/following-sibling::input")
    VertisElement input(@Param("filterName") String filterName);

}
