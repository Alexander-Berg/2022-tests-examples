package ru.auto.tests.desktop.element.cabinet.feeds;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SaleStatus extends VertisElement {

    @Name("Ссылка на подробности об ошибке")
    @FindBy("//span[contains(@class, 'Link') and .='{{ text }}']")
    VertisElement linkDetail(@Param("text") String text);

    @Name("Подробности ошибки")
    @FindBy("//span[contains(@class, 'FeedsDetailsOffersItem__errorMessage')]")
    VertisElement openDetails();

    @Name("Ссылка «{{ text }}»")
    @FindBy("//a[contains(@class, 'Link') and .='{{ text }}']")
    VertisElement linkVIN(@Param("text") String text);
}