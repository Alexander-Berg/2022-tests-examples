package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

public interface SitePlansModal extends AtlasWebElement {

    @Name("Инфа о застройщике в квартирографии")
    @FindBy("//div[contains(@class, 'SitePlansModal__developer')]")
    Link sitePlansDevInfo();

    @Name("Список планировок")
    @FindBy(".//a[contains(@class,'CardPlansOffersSerp__link')]")
    ElementsCollection<AtlasWebElement> cardPlansOffers();

    @Name("Картинка планировки")
    @FindBy(".//div[@class='SitePlansModal__planImage']")
    AtlasWebElement planImage();
}
