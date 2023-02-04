package ru.auto.tests.desktop.element.catalog.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface OpinionItem extends VertisElement {

    @Name("Ссылка с заголовка")
    @FindBy(".//a[contains(@class, 'carousel__title')]")
    VertisElement titleUrl();
}
