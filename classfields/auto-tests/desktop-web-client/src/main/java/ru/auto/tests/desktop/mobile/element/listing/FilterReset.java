package ru.auto.tests.desktop.mobile.element.listing;


import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterReset extends VertisElement {

    @Name("Заголовок блока")
    @FindBy(".//h1[@class = 'ListingResetFiltersSuggest__title'] |" +
            ".//p[@class = 'ListingResetFiltersSuggest__title']")
    VertisElement title();

    @Name("Подзаголовок блока")
    @FindBy(".//p[@class = 'ListingResetFiltersSuggest__subtitle']")
    VertisElement subTitle();

    @Name("Кнопка (ссылка) сбросить все фильтры")
    @FindBy(".//a[contains(@class, 'ListingResetFiltersSuggest__item') and " +
            ".//div[@class = 'ListingResetFiltersSuggest__item-label' and .='Сбросить все']]")
    VertisElement resetAllButton();


    @Name("Кнопока сброса фильтра {{ text }}")
    @FindBy(".//div[@class = 'ListingResetFiltersSuggest__item' and " +
            ".//div[@class = 'ListingResetFiltersSuggest__item-label' and .='{{ text }}']]")
    FilterResetButton resetButton(@Param("text") String Text);
}
