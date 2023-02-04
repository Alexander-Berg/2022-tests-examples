package ru.auto.tests.desktop.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GeoRadiusCounters extends VertisElement {

    @Name("Список гео-колец")
    @FindBy(".//div[@class = 'ListingGeoRadiusCounters__item'] | " +
            ".//div[@class = 'ListingGeoRadiusCounters__item ListingGeoRadiusCounters__item_active']")
    ElementsCollection<VertisElement> geoRadiusCountersList();

    @Step("Получаем гео-кольцо с индексом {i}")
    default VertisElement getGeoRadiusCounter(int i) {
        return geoRadiusCountersList().should(hasSize(greaterThan(i))).get(i);
    }

}
