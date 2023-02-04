package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import lombok.Getter;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SortBar extends VertisElement {

    @Name("Сортировка «{{ text }}»")
    @FindBy(".//div[contains(@class, 'ListingSortTabs__sort') and .='{{ text }}']")
    VertisElement sort(@Param("text") String text);

    @Name("Сортировка «{{ text }}»")
    @FindBy(".//a[contains(@class, 'tabs__item-title') and .='{{ text }}'] | " +
            ".//li[contains(@class, 'tabs__item') and .='{{ text }}']")
    VertisElement sortTab(@Param("text") String text);

    @Name("Количество предложений")
    @FindBy(".//div[contains(@class, 'ListingSortTabs__count')]")
    VertisElement offersCount();

    enum SortBy {
        FRESH_DESC("актуальности", "fresh_relevance_1-desc"),
        DATE_DESC("дате размещения", "cr_date-desc"),
        PRICE_DESC("убыванию цены", "price-desc"),
        PRICE_ASC("возрастанию цены", "price-asc"),
        YEAR_DESC("году: новее", "year-desc"),
        YEAR_ASC("году: старше", "year-asc"),
        RUN_ASC("пробегу", "km_age-asc"),
        NAME_ASC("названию", "alphabet-asc"),
        DISCOUNT_DESC("скидкам", "max_discount_absolute-desc"),
        AUTORU_EXCLUSIVE_DESC("уникальности", "autoru_exclusive-desc"),
        PRICE_PROFITABILITY_DESC("оценке стоимости", "price_profitability-desc"),
        PROWEN_OWNER_DESC("Сначала от собственников", "proven_owner-desc");

        @Getter
        private String name;

        @Getter
        private String alias;

        SortBy(String stringName, String alias) {
            this.name = stringName;
            this.alias = alias;
        }
    }
}