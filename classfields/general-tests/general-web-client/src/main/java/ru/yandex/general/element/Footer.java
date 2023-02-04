package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Footer extends VertisElement {

    @Name("Ссылка на категорию «{{ value }}»")
    @FindBy(".//ul[contains(@class, 'category')]/li/a[contains(., '{{ value }}')]")
    VertisElement category(@Param("value") String value);

    @Name("Ссылка на город «{{ value }}»")
    @FindBy(".//ul[contains(@class, 'cities')]/li/a[contains(., '{{ value }}')]")
    VertisElement city(@Param("value") String value);

    @Name("Сео текст")
    @FindBy("//footer[2]//span[contains(@class, 'Footer__seoText_')]")
    VertisElement seoText();

    @Name("Ссылка «Помощь»")
    @FindBy("(//footer | //footer[2])//a[contains(., 'Помощь')]")
    VertisElement help();

    @Name("Ссылка «Условия использования»")
    @FindBy("(//footer | //footer[2])//a[contains(., 'Условия использования')]")
    VertisElement terms();

}
