package ru.auto.tests.desktop.element.listing;


import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterReset extends VertisElement {

    @Name("Заголовок блока")
    @FindBy(".//p[@class = 'ListingResetFiltersSuggest__title']")
    VertisElement title();

    @Name("Подзаголовок блока")
    @FindBy(".//p[@class = 'ListingResetFiltersSuggest__subtitle']")
    VertisElement subTitle();

    @Name("Кнопка (ссылка) сбросить все фильтры")
    @FindBy(".//a[contains(@class, 'ListingResetFiltersSuggest__item-button')]")
    VertisElement resetAllButton();

    @Name("Кнопока сброса фильтра  «{{ text }}»")
    @FindBy(".//div[@class = 'ListingResetFiltersSuggest__item-button' and " +
            ".//div[@class = 'ListingResetFiltersSuggest__item-label-name' and .='{{ text }}']]")
    FilterResetButton resetButton(@Param("text") String Text);
}