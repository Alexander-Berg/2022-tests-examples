package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;

public interface PromoPage extends BasePage, WithCheckbox {

    @Name("Промо «Обнови приложение»")
    @FindBy("//div[@class='CreditForceAppUpdate']")
    VertisElement creditForceAppUpdatePromo();

    @Name("Крестик закрытия промо")
    @FindBy("//a[@class = 'FromWebToAppSplashWithPhone__close']")
    VertisElement closeButton();

    //loyalty
    @Name("Телефон")
    @FindBy("//div[contains(@class, 'PagePromoLoyalty__helpContent')]//a[1]")
    VertisElement loyaltyPhone();

    @Name("Ссылка «напишите нам»")
    @FindBy("//div[contains(@class, 'PagePromoLoyalty__helpContent')]//a[2]")
    VertisElement loyaltyHelpUrl();
}
