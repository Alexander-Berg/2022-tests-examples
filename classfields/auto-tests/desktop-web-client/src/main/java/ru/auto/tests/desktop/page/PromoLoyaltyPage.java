package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PromoLoyaltyPage extends PromoPage {

    @Name("Телефон")
    @FindBy("//div[contains(@class, 'PagePromoLoyalty__helpContent')]//a[1]")
    VertisElement phone();

    @Name("Ссылка «напишите нам»")
    @FindBy("//div[contains(@class, 'PagePromoLoyalty__helpContent')]//a[2]")
    VertisElement helpUrl();
}