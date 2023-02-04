package ru.auto.tests.desktop.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInputGroup;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithSelectGroup;
import ru.auto.tests.desktop.element.GenerationsPopup;
import ru.auto.tests.desktop.element.catalog.MarkModelGenBlock;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.not;

public interface Filter extends VertisElement,
        WithSelect,
        WithSelectGroup,
        WithInputGroup,
        WithCheckbox,
        WithActivePopup,
        WithButton {

    @FindBy(".//input[@name='price_from']")
    VertisElement priceFrom();

    @FindBy(".//input[@name='price_to']")
    VertisElement priceTo();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"body_key\"') or contains(@data-bem,'\"name\":\"autoru_body_type\"')]")
    VertisElement body();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"gear_type\"')]")
    VertisElement gear();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"engine_type\"')]")
    VertisElement engine();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"transmission_full\"')]")
    VertisElement transmission();

    @Name("Год от")
    @FindBy(".//Button[.//span[.= 'Год от']]")
    VertisElement yearFrom();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"year_to\"')]")
    VertisElement yearTo();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"displacement_from\"')]")
    VertisElement displacementFrom();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"displacement_to\"')]")
    VertisElement displacementTo();

    @FindBy(".//input[@name='power_from']")
    VertisElement powerFrom();

    @FindBy(".//input[@name='power_to']")
    VertisElement powerTo();

    @FindBy(".//div[contains(@data-bem,'\"name\":\"steering_wheel\"')]")
    VertisElement steeringWheel();

    @FindBy(".//div[contains(@data-bem,'\"text\":\"Количество мест\"') " +
            "or contains(@data-bem,'\"text\":\"Кол-во мест\"')]")
    VertisElement seats();

    @Name("Кнопка «Все параметры»")
    @FindBy(".//span[contains(., 'Все параметры')]")
    VertisElement showAdvancedFiltersButton();

    @Name("Кнопка «Свернуть»")
    @FindBy(".//span[contains(., 'Свернуть')]")
    VertisElement hideAdvancedFiltersButton();

    @Name("Цвет «{{ colorCode }}»")
    @FindBy(".//input[@value='{{ colorCode }}']/../..//div |" +
            ".//div[contains(@class, 'ColorFilterItem_color_{{ colorCode }}')]" +
            "//div[contains(@class, 'ColorFilterItem__color-checkbox')]")
    VertisElement color(@Param("colorCode") String colorCode);

    @FindBy(".//input[@name='acceleration_from']")
    VertisElement accelerationFrom();

    @FindBy(".//input[@name='acceleration_to']")
    VertisElement accelerationTo();

    @Name("Кнопка «Сбросить»")
    @FindBy(".//a[contains(@class,'search-form__advanced-reset')] | " +
            ".//a[contains(@class, '__linkReset')]")
    VertisElement resetButton();

    @Name("Кнопка «Показать»")
    @FindBy(".//button[contains(@class,'search-button')] |" +
            ".//button[contains(@class, 'Button_color_blue')]")
    VertisElement submitButton();

    @Name("Кнопка «Показать» или «Ничего не найдено»")
    @FindBy(".//button[contains(@class, 'search-button')] | .//div[contains(@class, 'search-button_empty')] | " +
            ".//button[contains(@class, 'Button_color_blue')] | .//div[contains(@class, '__submitButtonEmpty')]")
    VertisElement resultsButton();

    @Name("Кнопка «Ничего не найдено»")
    @FindBy(".//div[@class = 'ListingCarsFilters__submitButtonEmpty']")
    VertisElement emptyResultButton();

    @Name("Кнопка «+»")
    @FindBy(".//button[contains(@class, 'search-form-mmm-item__add')] |" +
            ".//div[contains(@class, 'MMMMultiFilterButtonAdd')]//button | " +
            ".//div[contains(@class, 'MMMFilter-module__MMMFilter__itemColumn')]/button |" +
            ".//div[contains(@class, 'MMMFilter__itemColumn_button')]")
    VertisElement addIcon();

    @Name("Кнопка сохранения поиска")
    @FindBy(".//*[contains(@class,'search-save ')] | " +
            ".//div[@class = 'ListingFilterSubscription-module__control'] | " +
            ".//div[contains(@class, 'SubscriptionSaveButton__button')]")
    VertisElement saveSearchButton();

    @FindBy("//div[contains(@class, 'search-form-v2-mmm')]")
    MarkModelGenBlock markModelGenBlock();

    @Name("Радио-кнопка «{{ value }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_button')][contains(., '{{ value }}')] | " +
            ".//label[contains(@class, 'radio_type_button')][contains(., '{{ value }}')]")
    VertisElement radioButton(@Param("value") String value);

    @Name("Радио-кнопка «{{ value }}»")
    @FindBy(".//label[contains(@class, 'Radio_checked')][contains(., '{{ value }}')]")
    VertisElement radioButtonSelected(@Param("value") String value);

    @Name("Инпут «{{ value }}»")
    @FindBy(".//input[contains(@class, 'TextInput__control') and preceding-sibling::div[contains(., '{{ value }}')]] | " +
            ".//input[contains(@class, 'input__control') and @placeholder = '{{ value }}'] | " +
            ".//input[contains(@class, 'TextInput__control') and ../../div[contains(., '{{ value }}')]]")
    VertisElement input(@Param("value") String value);

    @Name("Поп-ап поколений")
    @FindBy("//div[@class = 'GenerationFilter__popupContent']")
    GenerationsPopup generationsPopup();

    @Name("Залипающая панель")
    @FindBy(".//div[contains(@class, 'Filters__actions_sticky')]")
    VertisElement stickyPanel();

    enum Menu {
        MARK("Марка", "mark"),
        MODEL("Модель", "model"),
        GENERATION("Поколение", "super_gen"),
        BODY_TYPE("Кузов", "autoru_body_type"),
        GEARBOX("Коробка", "transmission_full"),
        ENGINE("Двигатель", "engine_type"),
        DRIVE("Привод", "gear_type"),
        ENGINE_DISPLACEMENT_FROM("Объем от", "displacement_from"),
        ENGINE_DISPLACEMENT_TO("до", "displacement_to"),
        YEAR_FROM("Год от", "year_from"),
        YEAR_TO("до", "year_to"),

        ADV_DAMAGE("Кроме битых", "beaten"),
        ADV_OWNING_TIME("Срок владения", "owning_time"),
        ADV_WHEEL("Руль", "steering_wheel"),
        ADV_STATE("Растаможен", "customs_state"),
        ADV_AIRBAGS("Подушки безопасности", "catalog_equipment[]\",\"text\":\"Подушки безопасности"),
        ADV_CLIMATE("Климат", "catalog_equipment[]\",\"text\":\"Климат"),
        ADV_WHEEL_ADJUSTMENT("Регулировка руля", "catalog_equipment[]\",\"text\":\"Регулировка руля"),
        ADV_UPHOLSTERY("Материал", "catalog_equipment[]\",\"text\":\"Материал"),
        ADV_ELECTRIC_LIFTS("Электроподъемники", "catalog_equipment[]\",\"text\":\"Электроподъемники"),
        ADV_SEEDS("Количесво мест", "catalog_equipment[]\",\"text\":\"Количесво мест"),
        ADV_ADJUSTING_SEAT_DRIVER("Регулировка сидения водителя", "catalog_equipment[]\",\"text\":\"Регулировка сидения водителя"),
        ADV_ADJUSTING_SEAT_PASSENGER("Регулировка сидения пассажира", "catalog_equipment[]\",\"text\":\"Регулировка сидения пассажира");

        private final String name;
        private final String alias;

        Menu(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    enum Input {
        RUN_FROM("Пробег от, км", "km_age_from"),
        RUN_TO("до", "km_age_to"),
        PRICE_FROM("Цена от, руб", "price_from"),
        PRICE_TO("до", "price_to"),
        ADV_POWER_FROM("Мощность от, л.с.", "power_from"),
        ADV_POWER_TO("до", "power_to"),
        ADV_ACCELERATION_FROM("Разгон от, с", "acceleration_from"),
        ADV_ACCELERATION_TO("до", "acceleration_to");

        private final String name;
        private final String alias;

        Input(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    @Step("Разворачиваем все параметры")
    default void showAdvancedFilters() {
        showAdvancedFiltersButton().should(WebElementMatchers.isDisplayed()).click();
        showAdvancedFiltersButton().waitUntil(not(WebElementMatchers.isDisplayed()));
        hideAdvancedFiltersButton().waitUntil(WebElementMatchers.isDisplayed());
    }

    @Step("Получаем количество найденных предложений")
    default int getFoundOffers() {
        return Integer.parseInt(submitButton().getText().replaceAll("\\D+", ""));
    }
}
