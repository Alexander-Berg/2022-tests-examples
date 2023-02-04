package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface PromoDealPage extends WithButton, PromoPage {

    @Name("Страница промо безопасной сделки")
    @FindBy("//div[@class = 'PromoPageSafeDeal__container']")
    VertisElement promoContent();

    @Name("Фаб")
    @FindBy("//div[contains(@class, 'PromoPageSafeDealHero__fab_visible')]")
    VertisElement fab();

}
