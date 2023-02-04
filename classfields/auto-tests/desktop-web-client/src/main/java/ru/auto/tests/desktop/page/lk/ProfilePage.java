package ru.auto.tests.desktop.page.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.page.BasePage;

public interface ProfilePage extends BasePage {

    @Name("Профиль")
    @FindBy(".//div[contains(@class, 'PublicProfile')]")
    VertisElement profile();

}
