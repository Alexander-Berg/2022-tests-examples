package ru.auto.tests.desktop.element.cabinet.walkin;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface EventItem extends VertisElement {

    @Name("Устройство")
    @FindBy(".//div[@class = 'WalkInEventsListItem__platform']")
    VertisElement platform();

    @Name("Список ссылок просмотренных объявлений")
    @FindBy(".//div[@class = 'WalkInEventsListItem__views']//a")
    ElementsCollection<VertisElement> viewLinkList();

    @Name("Интересуемые марки и модели")
    @FindBy(".//div[@class = 'WalkInEventsListItem__searchHistory']")
    VertisElement interestedMarkModels();

    @Name("Расширенное содержание")
    @FindBy(".//div[contains(@class, 'WalkInEventsListItem__expandableContent')]")
    ElementsCollection<VertisElement> expandableContent();

    @Name("Продает на авто.ру")
    @FindBy(".//div[@class = 'WalkInEventsListItem__userOffer']/div")
    VertisElement userOffer();

    @Step("Получаем ссылку просмотренного объявления с индексом {i}")
    default VertisElement getViewLink(int i) {
        return viewLinkList().should(hasSize(greaterThan(i))).get(i);
    }
}
