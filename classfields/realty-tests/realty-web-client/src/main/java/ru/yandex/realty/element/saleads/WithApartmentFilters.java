package ru.yandex.realty.element.saleads;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.element.saleads.popup.ExtendedFiltersPopup;

import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;


/**
 * @author kurau (Yuri Kalinin)
 */
public interface WithApartmentFilters {

    @Name("Блок фильтров")
    @FindBy("//form[contains(@class, 'FiltersForm')][not(contains(@class, 'FiltersForm_extra-expanded'))]")
    FiltersBlock filters();

    @Name("Расширенный блок фильтров")
    @FindBy("//form[contains(@class, 'FiltersForm')][contains(@class, 'FiltersForm_extra-expanded')]")
    ExtendedFiltersPopup extendFilters();

    @Step("Раскрываем расширенный фильтр")
    default void openExtFilter() {
        filters().waitUntil(WebElementMatchers.isDisplayed());
        filters().showMoreFiltersButton().click();
        extendFilters().waitUntil(WebElementMatchers.isDisplayed());
        waitSomething(2, TimeUnit.SECONDS);
    }

    @Step("Закрываем расширенный фильтр")
    default void closeExtFilter() {
        extendFilters().waitUntil(WebElementMatchers.isDisplayed());
        extendFilters().showMoreFiltersButton().click();
        filters().waitUntil(WebElementMatchers.isDisplayed());
        waitSomething(2, TimeUnit.SECONDS);
    }
}
