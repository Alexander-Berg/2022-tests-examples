package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.element.Notifier;

public interface WithNotifier {

    @Name("Нотифайка")
    @FindBy("//div[contains(@class, 'notifier_visible')] | " +
            "//div[contains(@class, 'Notifier_visible')]")
    Notifier notifier();

    @Name("Нотифайка с текстом «{{ text }}»")
    @FindBy("//div[contains(@class, 'notifier_visible') and .//div[.= '{{ text }}']] | " +
            "//div[contains(@class, 'Notifier_visible') and .//div[.= '{{ text }}']]")
    Notifier notifier(@Param("text") String Text);

}