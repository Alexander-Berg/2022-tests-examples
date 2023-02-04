package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;

public interface SortBar extends VertisElement, WithSelect, WithCheckbox, WithActivePopup {

    @Name("Переключатель на группировку по моделям")
    @FindBy(".//label[.//*[contains(@class, '_output-type-models')]]")
    VertisElement modelSwitcher();

    @Name("Переключатель на табличный вид")
    @FindBy(".//label[.//*[contains(@class, '_output-type-table')]]")
    VertisElement tableSwitcher();

    @Name("Переключатель на списочный вид")
    @FindBy(".//label[.//*[contains(@class, '_output-type-list')]]")
    VertisElement listSwitcher();

    enum SortBy {
        FRESH_DESC("По актуальности", "fresh_relevance_1-desc"),
        DATE_DESC("По дате размещения", "cr_date-desc"),
        PRICE_DESC("По убыванию цены", "price-desc"),
        PRICE_ASC("По возрастанию цены", "price-asc"),
        YEAR_DESC("По году: новее", "year-desc"),
        YEAR_ASC("По году: старше", "year-asc"),
        RUN_ASC("По пробегу", "km_age-asc"),
        NAME_ASC("По названию", "alphabet-asc"),
        DISCOUNT_DESC("По скидкам", "max_discount_absolute-desc"),
        AUTORU_EXCLUSIVE_DESC("По уникальности", "autoru_exclusive-desc"),
        PRICE_PROFITABILITY_DESC("По оценке стоимости", "price_profitability-desc"),
        PROWEN_OWNER_DESC("Сначала от собственников", "proven_owner-desc");

        private String name;
        private String alias;

        SortBy(String stringName, String alias) {
            this.name = stringName;
            this.alias = alias;
        }

        public String getAlias() {
            return alias;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    enum ListingType {
        LIST("Список"),
        TABLE("Таблица"),
        TILE("Плитки");

        private String name;

        ListingType(String title) {
            this.name = title;
        }

        public String getTitle() {
            return name();
        }

        @Override
        public String toString() {
            return getTitle();
        }
    }

    enum TopDaysBy {
        ALL("За все время", "off"),
        DAYS_1("За сутки", "1"),
        DAYS_2("За 2 дня", "2"),
        DAYS_3("За 3 дня", "3"),
        WEEK_1("За неделю", "7"),
        WEEK_2("За 2 недели", "14"),
        WEEK_3("За 3 недели", "21"),
        MONTH_1("За месяц", "31"),
        FRESH("Самые свежие", "hot");

        private String name;
        private String alias;

        TopDaysBy(String stringName, String alias) {
            this.name = stringName;
            this.alias = alias;
        }

        public String getAlias() {
            return alias;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
