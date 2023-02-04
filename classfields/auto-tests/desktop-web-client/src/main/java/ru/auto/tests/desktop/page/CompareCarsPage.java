package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.element.compare.Breadcrumbs;
import ru.auto.tests.desktop.element.compare.Head;
import ru.auto.tests.desktop.element.compare.Offers;
import ru.auto.tests.desktop.element.compare.Reviews;
import ru.auto.tests.desktop.element.compare.Row;

public interface CompareCarsPage extends BasePage, WithButton, WithCheckbox, WithSelect {

    @Name("Хлебные крошки")
    @FindBy("//ul[contains(@class, 'PageVersus__breadcrumbs')] |" +
            "//ul[contains(@class, 'VersusBreadcrumbs PageVersusDesktop__breadcrumbs')]")
    Breadcrumbs breadcrumbs();

    @Name("Верхний блок первой модели")
    @FindBy("//th[contains(@class, 'VersusHeadCell Versus__cell')][1]")
    Head firstModelHead();

    @Name("Верхний блок второй модели")
    @FindBy("//th[contains(@class, 'VersusHeadCell Versus__cell')][2]")
    Head secondModelHead();

    @Name("Блок с ценой второй модели")
    @FindBy("//tr[contains(@class, 'Versus__row')]/td[contains(@class, 'VersusPriceCell')][2]")
    VertisElement secondModelPriceBlock();

    @Name("Блок предложений первой модели")
    @FindBy("//td[contains(@class, 'VersusPriceCell Versus__cell')] |" +
            "//td[contains(@class, 'VersusPriceCell VersusPriceCell_desktop Versus__cell')]")
    Offers firstModelOffers();

    @Name("Отзывы первой модели")
    @FindBy("//td[contains(@class, 'VersusRatingCell')]|" +
            "//td[contains(@class, 'VersusRatingCell VersusRatingCell_desktop Versus__cell')]")
    Reviews firstModelReviews();

    @Name("Строка «{{ text }}» в характеристиках")
    @FindBy(".//tr[@class = 'ComparisonRow' and .//div[.= '{{ text }}']] |" +
            ".//tr[@class='ComparisonRow ComparisonRow_desktop' and .//div[.= '{{ text }}']]")
    Row row(@Param("text") String text);
}