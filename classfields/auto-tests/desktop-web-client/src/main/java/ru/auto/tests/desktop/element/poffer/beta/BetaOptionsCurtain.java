package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithSelect;

public interface BetaOptionsCurtain extends VertisElement, WithButton, WithSelect, WithCheckbox, WithInput {

    String SEARCH = "Поиск";

    @Name("Список всех опций")
    @FindBy(".//div[@class='CatalogEquipmentFiltersGroup__item']")
    ElementsCollection<VertisElement> optionsList();

    @Name("Подвал шторки")
    @FindBy(".//div[@class='OfferFormEquipmentFieldCurtain__footer']")
    BetaOptionsCurtainFooter footer();

    @Name("Иконка закрытия")
    @FindBy(".//*[contains(@class, 'Curtain__closer_big')]")
    VertisElement closeIcon();
}