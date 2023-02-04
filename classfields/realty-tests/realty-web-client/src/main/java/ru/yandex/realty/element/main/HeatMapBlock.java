package ru.yandex.realty.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

/**
 * @author kantemirov
 */
public interface HeatMapBlock extends Link {

    @Name("Кнопка свайпа назад")
    @FindBy(".//button[contains(@class, 'SwipeableBlock__back')]")
    AtlasWebElement swipeBack();

    @Name("Кнопка свайпа вперед")
    @FindBy(".//button[contains(@class, 'SwipeableBlock__forward')]")
    AtlasWebElement swipeForward();
}
