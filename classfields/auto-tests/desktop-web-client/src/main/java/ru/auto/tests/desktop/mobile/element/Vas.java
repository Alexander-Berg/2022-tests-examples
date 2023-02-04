package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Description;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 15.02.18
 */
public interface Vas extends VertisElement {

    @Description("Услуга «{{ name }}»")
    @FindBy(".//div[(contains(@class, 'sales__vas-package sales__') or contains(@class, 'sales__vas-service-row')) and contains(., '{{ name }}')] | " +
            ".//div[contains(@class, 'VasItem') and .//*[.= '{{ name }}']]")
    VasService service(@Param("name") String name);

    @Description("Подключённая услуга «{{ name }}»")
    @FindBy(".//div[contains(@class, 'VasItemService') and .//div[.= '{{ name }}']] | " +
            ".//div[contains(@class, 'VasItemPackage') and .//div[.= '{{ name }}']]")
    VasActiveService activeService(@Param("name") String name);
}