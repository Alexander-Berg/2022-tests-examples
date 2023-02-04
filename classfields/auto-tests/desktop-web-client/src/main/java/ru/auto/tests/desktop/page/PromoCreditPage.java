package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.page.PromoPage;

public interface PromoCreditPage extends PromoPage {

    @Name("Заголовок")
    @FindBy("//div[@class = 'CreditPromoBrokerForm__description']")
    VertisElement creditHeader();

    @Name("Форма")
    @FindBy("//div[@class = 'CreditPromoBrokerForm__form']")
    VertisElement form();
}
