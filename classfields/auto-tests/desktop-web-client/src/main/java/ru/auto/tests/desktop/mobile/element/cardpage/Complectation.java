package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

/**
 * Created by kopitsa on 18.09.17.
 */
public interface Complectation extends VertisElement, WithButton {

    @Name("Опция «{{ text }}»")
    @FindBy(".//div[@class = 'ComplectationGroups__group' and contains(., '{{ text }}')] | " +
            ".//header[contains(@class, 'OfferAmpComplectation') and contains(., '{{ text }}')] |" +
            ".//div[@class = 'ComplectationGroups__item' and contains(., '{{ text }}')]")
    VertisElement option(@Param("text") String text);

    @Name("Кнопка «Яндекс.Авто»")
    @FindBy(".//a[contains(@class, 'section__titleBanner')] | " +
            ".//a[contains(@class, 'CardComplectationTitleBanner')]")
    VertisElement yandexAutoButton();
}