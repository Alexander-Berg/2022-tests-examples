package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface CardSectionExpandable extends AtlasWebElement {

    @Name("Стрелка")
    @FindBy(".//i[contains(@class, 'animate-direction')]")
    AtlasWebElement arrowIcon();

    @Name("Заголовок")
    @FindBy(".//div[@class='CardSection__header']")
    AtlasWebElement header();

    @Name("Контент")
    @FindBy(".//div[contains(@class, 'CardSection__content')]")
    AtlasWebElement content();

    @Name("Контент новостройки")
    @FindBy("//div[contains(@class, 'NewbuildingCardAccordion__content' ) and not(contains(@class, 'Hidden'))]")
    AtlasWebElement newbuildingContent();

    @Name("Скрытый контент новостройки")
    @FindBy("//div[contains(@class, 'NewbuildingCardAccordion__contentHidden')]")
    AtlasWebElement newbuildingContentHidden();
}
