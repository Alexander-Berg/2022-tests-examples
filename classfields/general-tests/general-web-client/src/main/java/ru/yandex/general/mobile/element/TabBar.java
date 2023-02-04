package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface TabBar extends VertisElement {

    @Name("Кнопка подачи оффера")
    @FindBy(".//a[contains(., 'Разместить')]")
    VertisElement addOffer();

    @Name("Задизейбленная кнопка подачи оффера")
    @FindBy(".//*[contains(@class, 'addButtonDisabled')]")
    VertisElement disabledAddOffer();

    @Name("Кнопка «Избранное»")
    @FindBy(".//a[contains(., 'Избранное')]")
    VertisElement favorites();

    @Name("Кнопка «Мои объявления»")
    @FindBy(".//a[contains(., 'Мои объявления')]")
    VertisElement myOffers();

    @Name("Кнопка «Главная»")
    @FindBy(".//a[contains(., 'Главная')]")
    VertisElement mainPage();

}
