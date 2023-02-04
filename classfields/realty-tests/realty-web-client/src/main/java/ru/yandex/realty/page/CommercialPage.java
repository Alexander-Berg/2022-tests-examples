package ru.yandex.realty.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.FiltersBlock;
import ru.yandex.realty.element.saleads.popup.ExtendedFiltersPopup;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * Created by kopitsa on 06.07.17.
 */
public interface CommercialPage extends OffersSearchPage {
//
//    @Name("Блок фильтров")
//    @FindBy("//form[contains(@class, 'FiltersForm_category_COMMERCIAL')]" +
//            "[not(contains(@class, 'FiltersForm_extra-expanded'))]")
//    FiltersBlock commercialFilters();
//
//    @Name("Блок фильтров")
//    @FindBy("//form[contains(@class, 'FiltersForm_category_COMMERCIAL')]" +
//            "[contains(@class, 'FiltersForm_extra-expanded')]")
//    ExtendedFiltersPopup extendFilters();
//
//    @Name("Блок расширенных фильтров")
//    @FindBy("//div[contains(@class,'FiltersForm__extra')]")
//    ExtendedFiltersPopup extendFilters();
//
//    @Step("Раскрываем комерческий фильтр")
//    default void openExtFilter() {
//        commercialFilters().waitUntil(isDisplayed());
//        commercialFilters().showMoreFiltersButton().click();
//        extendFilters().waitUntil(isDisplayed());
//    }
}
