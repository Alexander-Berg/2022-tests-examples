package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Footer extends VertisElement, WithButton {

    @Name("Ссылка региона")
    @FindBy(".//div[contains(@class, 'row_popular')]//a[contains(text(), '{{ region }}')] | " +
            ".//div[contains(@class, 'Footer__geo-links-main-regions')]//a[.='{{ region }}']")
    VertisElement popularRegionLinkByRegion(@Param("region") String region);

    @Name("Ссылка на ВК")
    @FindBy(".//a[contains(@class, 'link_vk')] | " +
            ".//a[contains(@href, 'vk')]")
    VertisElement vkUrl();

    @Name("Ссылка на YouTube")
    @FindBy(".//a[contains(@class, 'link_yt')] | " +
            ".//a[contains(@href, 'yt')]")
    VertisElement youTubeUrl();

    @Name("Ссылка на Одноклассники")
    @FindBy(".//a[contains(@class, 'link_ok')]")
    VertisElement okUrl();

    @Name("Ссылка на Яндекс")
    @FindBy(".//a[contains(@class, 'FooterLogo')] | " +
            ".//a[contains(@class, 'footer__ya-logo')]")
    VertisElement yandexUrl();

    @Name("Копирайт")
    @FindBy(".//span[contains(@class, 'Footer__copyright')] | " +
            "//span[contains(@class, 'FooterCopyright')]")
    VertisElement copyright();
}
