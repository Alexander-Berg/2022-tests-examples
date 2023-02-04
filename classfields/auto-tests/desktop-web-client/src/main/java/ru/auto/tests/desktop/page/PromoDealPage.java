package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.page.PromoPage;

public interface PromoDealPage extends PromoPage {

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'PromoPageSafeDeal__container']")
    VertisElement promoContent();
}
