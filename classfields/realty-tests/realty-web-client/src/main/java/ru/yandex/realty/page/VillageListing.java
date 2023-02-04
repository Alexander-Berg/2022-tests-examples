package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.WithApartmentFilters;
import ru.yandex.realty.element.saleads.WithShowMoreLink;
import ru.yandex.realty.element.village.VillageSerpItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VillageListing extends BasePage, WithShowMoreLink, WithApartmentFilters {

    @Name("Список котттеджных поселков")
    @FindBy("//div[contains(@class,'VillagesSerp__snippet')]")
    ElementsCollection<VillageSerpItem> villageSerpItems();

    default VillageSerpItem offer(int i) {
        return villageSerpItems().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
