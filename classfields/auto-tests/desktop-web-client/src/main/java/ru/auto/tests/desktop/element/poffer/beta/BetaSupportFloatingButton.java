package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BetaSupportFloatingButton extends VertisElement {

    @Name("Кнопка чата JivoSite")
    @FindBy(".//div[@class='JivoSiteChat__button']")
    VertisElement jivoSiteChatButton();

    @Name("Iframe чата JivoSite")
    @FindBy(".//iframe[@class='JivoSiteChat__frame']")
    VertisElement jivoSiteChatFrame();

}