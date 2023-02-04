package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.element.cabinet.Calendar;
import ru.auto.tests.desktop.element.cabinet.walkin.DescriptionBlock;
import ru.auto.tests.desktop.element.cabinet.walkin.DistributionInfoBlock;
import ru.auto.tests.desktop.element.cabinet.walkin.EventItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetWalkInPage extends BasePage, WithPager {

    @Name("Кнопка открытия календаря")
    @FindBy("//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();

    @Name("Календарь")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Calendar calendar();

    @Name("Общее количество приездов за выбранный период")
    @FindBy(".//div[@class = 'WalkInHeader__header']")
    VertisElement titleVisitCount();

    @Name("Блок с описанием страницы")
    @FindBy(".//div[@class = 'WalkInHeader__descriptionContainer']")
    DescriptionBlock descriptionBlock();

    @Name("Кнопка «Как это работает»")
    @FindBy(".//div[@class = 'WalkInFilters__helpButton']/a")
    VertisElement helpButton();

    @Name("График визитов в салон")
    @FindBy(".//div[@class = 'Graph']")
    VertisElement graph();

    @Name("Поп-ап с подсказкой на графике визитов")
    @FindBy(".//div[@class = 'GraphTooltip__container']")
    VertisElement graphTooltip();

    @Name("Блок информации о распределении по полу и возрасту (в развернутом состоянии)")
    @FindBy(".//div[@class = 'WalkIn__legend']/div[@class = 'WalkInCollapseBlock']")
    DistributionInfoBlock distributionInfoBlockExpanded();

    @Name("Блок информации о распределении по полу и возрасту (в свернутом состоянии)")
    @FindBy(".//div[@class = 'WalkIn__legend']/div[@class = 'WalkInCollapseBlock WalkInCollapseBlock_collapsed']")
    DistributionInfoBlock distributionInfoBlockCollapsed();

    @Name("Дата страницы")
    @FindBy(".//div[@class = 'WalkInEventsList__date']")
    VertisElement eventsDate();

    @Name("Список событий")
    @FindBy(".//div[@class = 'WalkInEventsList__item']")
    ElementsCollection<EventItem> eventList();

    @Step("Получаем событие с индексом {i}")
    default EventItem getEventItem(int i) {
        return eventList().should(hasSize(greaterThan(i))).get(i);
    }
}
