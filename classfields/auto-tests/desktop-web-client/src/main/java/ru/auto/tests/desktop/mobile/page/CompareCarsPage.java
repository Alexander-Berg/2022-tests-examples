package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithBreadcrumbs;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithMmmPopup;
import ru.auto.tests.desktop.mobile.component.WithSelect;
import ru.auto.tests.desktop.mobile.element.compare.Head;
import ru.auto.tests.desktop.mobile.element.compare.Related;
import ru.auto.tests.desktop.mobile.element.compare.Reviews;
import ru.auto.tests.desktop.mobile.element.compare.Row;

public interface CompareCarsPage extends BasePage, WithButton, WithCheckbox, WithSelect, WithBreadcrumbs, WithMmmPopup {

    @Name("Верхний блок")
    @FindBy("//thead")
    Head head();

    @Name("Блок предложений первой модели")
    @FindBy("//a[contains(@class, 'VersusPriceCell__link')]")
    VertisElement firstModelOffers();

    @Name("Отзывы первой модели")
    @FindBy("//td[contains(@class, 'VersusRatingCell_mobile Versus__cell')]")
    Reviews firstModelReviews();

    @Name("Строка «{{ text }}» в характеристиках")
    @FindBy(".//tr[@class = 'ComparisonRow ComparisonRow_mobile' and .//div[.= '{{ text }}']]")
    Row row(@Param("text") String text);

    @Name("Похожие")
    @FindBy("//div[contains(@class, 'VersusRelatedOffers__list')]")
    Related related();

    @Name("Блок «Всё о модели»")
    @FindBy("//div[contains(@class, '_crossLinks')]")
    VertisElement aboutModel();
}
