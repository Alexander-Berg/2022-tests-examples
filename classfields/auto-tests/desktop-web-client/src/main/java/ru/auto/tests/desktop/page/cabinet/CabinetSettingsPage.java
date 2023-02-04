package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.cabinet.settings.Section;

public interface CabinetSettingsPage extends BasePage, WithNotifier {

    @Name("Секция «{{ name }}»")
    @FindBy("//div[@class = 'SettingsOffers__section' and ./div[contains(., '{{ name }}')]]")
    Section section(@Param("name") String name);
}
