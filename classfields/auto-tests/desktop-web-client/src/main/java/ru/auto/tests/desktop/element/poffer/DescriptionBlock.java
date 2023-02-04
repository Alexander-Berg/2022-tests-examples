package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DescriptionBlock extends VertisElement {

    String ADD_AVITO_DESCRIPTION = "Добавить альтернативное описание на Авито";
    String ADD_DROM_DESCRIPTION = "Добавить альтернативное описание на Дром";

    @Name("Описание на Авито")
    @FindBy(".//div[contains(@class, 'description-classified_classified_avito')]")
    Block avitoDescription();

    @Name("Описание на Дром")
    @FindBy(".//div[contains(@class, 'description-classified_classified_drom')]")
    Block dromDescription();

}
