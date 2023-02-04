package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.cabinet.Footer;
import ru.auto.tests.desktop.element.cabinet.WithMenuPopup;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 17.05.18
 */
public interface CalculatorPage extends WebPage {

    @Name("Поп-ап с меню")
    @FindBy("//div[contains(@class, 'Popup_visible') and .//div[contains(@class, 'MenuItem')]]")
    WithMenuPopup withMenuPopup();

    @Name("Футер")
    @FindBy("//div[@class = 'Footer__container']")
    Footer footer();
}
