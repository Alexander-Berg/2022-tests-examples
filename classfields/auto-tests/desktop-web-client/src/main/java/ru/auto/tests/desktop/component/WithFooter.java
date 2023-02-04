package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Footer;

public interface WithFooter {

    @Name("Футер")
    @FindBy("//div[@class = 'Footer'] | " +
            "//footer | " +
            "//div[contains(@class, 'MagFooter')] | " +
            "//div[contains(@class, 'Footer Footer')]")
    Footer footer();
}