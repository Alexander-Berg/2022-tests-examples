package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.06.18
 */
public interface Footer extends VertisElement, WithButton {

    @Name("Ссылка на социальную сеть {{ text }}")
    @FindBy(".//a[contains(@href, '{{ text }}.com')]")
    VertisElement socialUrl(@Param("text") String name);

    @Name("Значок социальной сети Telegram")
    @FindBy(".//*[contains(@class, 'footer-tg')]")
    VertisElement telegramButton();

    @Name("")
    @FindBy(".//a[contains(@class, 'yandexLink')]")
    VertisElement yaLogo();
}
