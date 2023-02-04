package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.cabinet.settings.WhiteListControls;
import ru.auto.tests.desktop.element.cabinet.settings.WhiteListPhone;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetSettingsWhiteListPage extends BasePage, WithNotifier, WithButton {

    @Name("Поп-ап")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    Popup popup();

    @Name("Контент страницы")
    @FindBy("//div[contains(@class, 'SettingsWhitelist')]")
    VertisElement pageContent();

    @Name("Управление всеми телефонами")
    @FindBy("//div[contains(@class, 'Whitelist__controls')]")
    WhiteListControls controls();

    @Name("Список телефонов")
    @FindBy("//div[contains(@class, 'Whitelist__item')]")
    ElementsCollection<WhiteListPhone> phones();

    @Step("Получаем телефон с индексом {i}")
    default WhiteListPhone getPhone(int i) {
        return phones().should(hasSize(greaterThan(i))).get(i);
    }
}
