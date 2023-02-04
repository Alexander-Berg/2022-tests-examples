package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.WithInput;
import ru.auto.tests.desktop.mobile.element.filters.Mmm;
import ru.auto.tests.desktop.mobile.element.filters.Tags;

public interface ParamsPopup extends VertisElement, WithRadioButton, WithInput, WithCheckbox, WithButton {

    @Name("Кнопка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'FiltersPopup__close')]")
    VertisElement closeButton();

    @Name("Кнопка «Сбросить»")
    @FindBy(".//div[contains(@class, 'FiltersPopup__reset')]")
    VertisElement resetButton();

    @Name("Регион")
    @FindBy(".//div[contains(@class, 'GeoSelectMobile__region')]")
    VertisElement region();

    @Name("Радиус «{{ text }}»")
    @FindBy(".//button[contains(@class, 'Button_radius_round') and .= '{{ text }}']")
    VertisElement radius(@Param("text") String Text);

    @Name("Выбранный радиус «{{ text }}»")
    @FindBy(".//button[contains(@class, 'Button_radius_round') and contains(@class, 'Button_checked') and .= '{{ text }}']")
    VertisElement radiusSelected(@Param("text") String Text);

    @Name("Задизабленный радиус «{{ text }}»")
    @FindBy(".//button[contains(@class, 'Button_radius_round') and contains(@class, 'Button_disabled') and .= '{{ text }}']")
    VertisElement radiusDisabled(@Param("text") String Text);

    @Name("Параметр «{{ text }}»")
    @FindBy("//div[contains(@class, 'ListingFiltersItem') and .//div[.= '{{ text }}']]")
    VertisElement param(@Param("text") String Text);

    @Name("Кнопка открытия поп-апа «{{ text }}»")
    @FindBy("//div[contains(@class, 'AbstractPopupFilter') and .//div[.= '{{ text }}']]")
    VertisElement popupButton(@Param("text") String Text);

    @Name("Категория «{{ text }}»")
    @FindBy(".//div[contains(@class, 'CategorySelector')]//button[.= '{{ text }}']")
    VertisElement category(@Param("text") String Text);

    @Name("Подкатегория «{{ text }}»")
    @FindBy(".//div[contains(@class, 'IndexSubcategorySelectorItem') and .= '{{ text }}']")
    VertisElement subCategory(@Param("text") String Text);

    @Name("Секция «{{ text }}»")
    @FindBy(".//div[contains(@class, 'ListingFiltersPopup__section')]//button[.= '{{ text }}']")
    VertisElement section(@Param("text") String Text);

    @Name("Блок «Марка и модель»")
    @FindBy(".//div[contains(@class, 'ListingFiltersPopupSection') and .//div[.= 'Марка и модель']]")
    Mmm mmmBlock();

    @Name("Кнопка применения фильтров")
    @FindBy(".//div[contains(@class, 'FiltersPopup__bottom')]//button")
    VertisElement applyFiltersButton();

    @Name("Тэги в блоке «{{ text }}»")
    @FindBy("//div[contains(@class, 'ListingFiltersItem_opened') and .//div[.= '{{ text }}']]")
    Tags tags(@Param("text") String text);

    @Name("Тэги-пресеты")
    @FindBy("//div[contains(@class, 'ListingFiltersSearchTags')]")
    Tags tagsPresets();

    @Name("Тэги от «{{ text }}»")
    @FindBy("//div[contains(@class, 'ListingFiltersItem_opened') and .//div[.= '{{ text }}']]" +
            "//div[contains(@class, 'filter_from')]")
    Tags tagsFrom(@Param("text") String text);

    @Name("Тэги до «{{ text }}»")
    @FindBy("//div[contains(@class, 'ListingFiltersItem_opened') and .//div[.= '{{ text }}']]" +
            "//div[contains(@class, 'filter_to')]")
    Tags tagsTo(@Param("text") String text);

    @Name("Инпут от «{{ text }}»")
    @FindBy("(//div[.//div[contains(., '{{ text }}')] and contains(@class, 'FilterElements__row')]//input)[1] | " +
            "(//div[.//div[contains(., '{{ text }}')] and contains(@class, 'ListingFiltersCarsPublicApi__row')]//input)[1] | " +
            "//div[contains(@class, 'ListingFiltersItem_opened') and .//div[.= '{{ text }}']]//input |" +
            "//div[contains(@class, 'ListingFiltersPopupSection') and .//div[.= '{{ text }}']]//input")
    VertisElement inputFrom(@Param("text") String text);

    @Name("Инпут до «{{ text }}»")
    @FindBy("(//div[.//div[contains(., '{{ text }}')] and contains(@class, 'FilterElements__row')]//input)[2] | " +
            "(//div[.//div[contains(., '{{ text }}')] and contains(@class, 'FiltersCarsPublicApi__row')]//input)[2] | " +
            "(//div[contains(@class, 'ListingFiltersItem_opened') and .//div[.= '{{ text }}']]//input)[2] |" +
            "(//div[contains(@class, 'ListingFiltersPopupSection') and .//div[.= '{{ text }}']]//input)[2]")
    VertisElement inputTo(@Param("text") String text);

    @Name("Неактивный тумблер «{{ text }}»")
    @FindBy("//span[.= '{{ text }}']/..//label[contains(@class, 'Toggle') and not(contains(@class, 'Toggle_checked'))]")
    VertisElement inactiveToggle(@Param("text") String text);

    @Name("Aктивный тумблер «{{ text }}»")
    @FindBy("//span[.= '{{ text }}']/..//label[contains(@class, 'Toggle_checked')]")
    VertisElement activeToggle(@Param("text") String text);

    @Name("Слайдер платежа От")
    @FindBy(".//div[contains(@class, 'CreditFilterDetailsDump__row')][1]//div[contains(@class, 'Slider__toggler_from')]")
    VertisElement paymentSliderFrom();

    @Name("Слайдер платежа До")
    @FindBy(".//div[contains(@class, 'CreditFilterDetailsDump__row')][1]//div[contains(@class, 'Slider__toggler_to')]")
    VertisElement paymentSliderTo();

    @Name("Слайдер срока кредита")
    @FindBy(".//div[contains(@class, 'CreditFilterDetailsDump__row')][2]//div[contains(@class, 'Slider__toggler_to')]")
    VertisElement yearSliderTo();
}
