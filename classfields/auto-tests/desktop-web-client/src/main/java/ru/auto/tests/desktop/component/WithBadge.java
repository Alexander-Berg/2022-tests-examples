package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithBadge extends VertisElement {

    String PHOTO_URL = "https://images.mds-proxy.test.avto.ru/get-autoru-vos/2171969/023fb9f39f18cb4818f4627eac2e9db9/orig";

    @Name("Бейдж «{{ text }}» на фото")
    @FindBy(".//div[contains(@class, 'badge_') and contains(., '{{ text }}')] | " +
            ".//div[contains(@class, 'Badge_') and contains(., '{{ text }}')]")
    VertisElement badge(@Param("text") String text);

    @Name("Бейдж «Открыть оригинал» на фото")
    @FindBy(".//div[@data-badge = 'orig_image']")
    VertisElement badgeOpenOrig();
}
