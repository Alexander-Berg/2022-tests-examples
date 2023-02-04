package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInputGroup;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithSelectGroup;

public interface MotoBlock extends VertisElement, WithSelect, WithSelectGroup, WithInputGroup, WithButton {

    @Name("Блок ссылок")
    @FindBy(".//div[contains(@class, 'IndexLinks')]")
    VertisElement urlsBlock();

    @Name("Блок «Подбор мотоцикла»")
    @FindBy(".//div[contains(@class, 'IndexMotoFilters Index__col')]")
    VertisElement filtersBlock();

    @Name("Кнопка «Показать» или «Ничего не найдено»")
    @FindBy(".//div[@class = 'IndexMotoFilters__submit']/button")
    VertisElement resultsButton();

    @Name("Баннер")
    @FindBy(".//div[contains(@class, 'IndexMoto__teaser')]/..//div[@class = 'IndexTeaser__content']")
    VertisElement banner();

    @Name("Название баннера")
    @FindBy(".//div[contains(@class, 'IndexMoto__teaser')]/..//div[@class = 'IndexTeaser__title']")
    VertisElement bannerTitle();
}
