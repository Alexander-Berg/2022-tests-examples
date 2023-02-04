package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.garage.PromoItem;

public interface GarageAllPromoPage extends BasePage {

    String COPY_PROMOCODE = "Скопировать промокод";

    @Name("Список супер промо")
    @FindBy(".//div[contains(@class, '_listItem') and contains(@class, 'GaragePromoSuper')]")
    ElementsCollection<PromoItem> superPromoList();

    @Name("Список обычных промо")
    @FindBy(".//div[contains(@class, '_listItem') and contains(@class, 'GaragePromoRegular')]")
    ElementsCollection<PromoItem> regularPromoList();

}
