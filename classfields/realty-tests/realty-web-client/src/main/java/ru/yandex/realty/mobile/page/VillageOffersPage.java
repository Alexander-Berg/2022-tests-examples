package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.mobile.element.VillageOffer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VillageOffersPage extends BasePage {

    @Name("Список объекотв коттеджного поселка")
    @FindBy("//li[contains(@class,'VillagesSerp__item')]")
    ElementsCollection<VillageOffer> offers();

    default VillageOffer offer(int i) {
        return offers().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
