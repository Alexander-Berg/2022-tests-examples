package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithSelectGroup;

public interface SocialBlock extends VertisElement, WithSelect, WithSelectGroup {

    @Name("Ссылка на ВК")
    @FindBy(".//a[contains(@class, 'IndexSocialLinks__item_vk')]")
    VertisElement vkUrl();

    @Name("Ссылка на Одноклассники")
    @FindBy(".//a[contains(@class, 'IndexSocialLinks__item_ok')]")
    VertisElement okUrl();

}
