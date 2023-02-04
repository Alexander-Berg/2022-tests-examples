package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Pager extends VertisElement, WithButton {

    String SHOW_MORE = "Показать ещё";

    @Name("Кнопка: 'Следующая, Ctrl →'")
    @FindBy(".//button[contains(@class,'next')] | " +
            ".//a[contains(@class,'__next')]")
    VertisElement next();

    @Name("Кнопка: '← Ctrl, предыдущая'")
    @FindBy(".//button[contains(@class,'prev')] | " +
            ".//a[contains(@class,'__previous')]")
    VertisElement prev();

    @Name("Кнопка страницы: '{{ num }}'")
    @FindBy(".//span[contains(@class, 'radio-group')]//button[.='{{ num }}'] | " +
            ".//a[contains(@class, 'ListingPagination__page')]//span[.='{{ num }}']")
    VertisElement page(@Param("num") String num);

    @Name("Первая кнопка '...'")
    @FindBy(".//span[contains(@class, 'radio-group')]//button[.='…'] | " +
            ".//a[contains(@class, 'ListingPagination__page')]//span[.='…']")
    VertisElement threeDotsFirst();

    @Name("Последняя кнопка '...'")
    @FindBy("(.//span[contains(@class, 'radio-group')]//button[. = '…'])[last()] | " +
            "(.//a[contains(@class, 'ListingPagination__page')][contains(., '…')])[last()]")
    VertisElement threeDotsLast();

    @Name("Текущая страница")
    @FindBy(".//button[contains(@class, 'button_checked')] | " +
            ".//a[contains(@class, 'Button_disabled')]")
    VertisElement currentPage();

    @Name("Последняя страница")
    @FindBy("(.//span[contains(@class, 'radio-group')]//button)[last()] | " +
            "(.//a[contains(@class, 'ListingPagination__page')]//span)[last()]")
    VertisElement lastPage();
}
