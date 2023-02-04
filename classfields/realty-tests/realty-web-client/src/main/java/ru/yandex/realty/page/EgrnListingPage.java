package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.management.EgrnListingSnippet;

import static org.hamcrest.Matchers.hasSize;

public interface EgrnListingPage extends BasePage {

    @Name("Сниппеты ЕГРН отчета")
    @FindBy("//div[./div[contains(@class,'EgrnReport__snippet')]]")
    ElementsCollection<EgrnListingSnippet> egrnSnippets();

    @Name("Кнопка «Посмотреть квартиры»")
    @FindBy(".//a[contains(@class,'EgrnEmpty__button')]")
    AtlasWebElement watchFlats();

    @Name("Страница ошибки")
    @FindBy(".//div[contains(@class,'EgrnReportsProblem__wrapper')]")
    Link egrnErrorPage();

    default EgrnListingSnippet firstEgrnSnippet() {
        return egrnSnippets().waitUntil(hasSize(1)).get(0);
    }
}
