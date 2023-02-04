package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BannerContinueInApp extends VertisElement {

    @Name("Ссылка «Скачать»")
    @FindBy(".//a[.='Скачать']")
    VertisElement downloadLink();

}
