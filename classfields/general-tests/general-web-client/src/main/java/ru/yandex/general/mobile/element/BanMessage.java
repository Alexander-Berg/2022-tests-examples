package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.mobile.element.BanReason;
import ru.yandex.general.mobile.element.Link;

public interface BanMessage extends VertisElement {

    @Name("Тайтл")
    @FindBy(".//div[contains(@class, 'h3')]")
    VertisElement title();

    @Name("Список причин бана")
    @FindBy(".//li[contains(@class, 'reason')]")
    ElementsCollection<BanReason> banReasons();

    @Name("Кнопка «Написать в поддержку»")
    @FindBy(".//div[contains(@class, 'BanMessage__btn')]")
    Link chatWithSupport();

}
