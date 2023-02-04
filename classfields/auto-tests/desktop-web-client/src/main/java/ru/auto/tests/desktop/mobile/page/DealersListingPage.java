package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithGeoPopup;
import ru.auto.tests.desktop.mobile.component.WithGeoRadiusPopup;
import ru.auto.tests.desktop.mobile.component.WithMmmPopup;
import ru.auto.tests.desktop.mobile.element.Filters;
import ru.auto.tests.desktop.mobile.element.WithInput;
import ru.auto.tests.desktop.mobile.element.dealers.listing.DealersListItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DealersListingPage extends BasePage, WithMmmPopup, WithInput, WithGeoPopup, WithGeoRadiusPopup {

    @Name("Список дилеров")
    @FindBy("//div[@class = 'dealer-list-item']")
    ElementsCollection<DealersListItem> dealersList();

    default DealersListItem getDealer(int i) {
        return dealersList().should(hasSize(greaterThan(0))).get(0);
    }

    @Name("Дилер «{{ text }}» в саджесте")
    @FindBy("//div[@id='react-autowhatever-1']//div[text() = '{{ text }}']")
    VertisElement suggestItem(@Param("text") String text);

    @Name("Кнопка очистки инпута «Название дилера»")
    @FindBy("//i[contains(@class, 'Input__clear_visible')]")
    VertisElement clearInput();

    @Name("Фильтры")
    @FindBy("//div[contains(@class, 'DealersListingHead')]")
    Filters filters();

    @Name("Счётчик дилеров")
    @FindBy("//div[contains(@class, 'DealersListingSeo__results-count')]")
    VertisElement dealersCount();

    @Name("Иконка карты")
    @FindBy("//div[@class = 'page-dealers-listing__show-map']")
    VertisElement mapIcon();
}