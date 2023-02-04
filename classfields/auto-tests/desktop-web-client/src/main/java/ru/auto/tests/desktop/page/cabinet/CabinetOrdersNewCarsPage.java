package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Pager;
import ru.auto.tests.desktop.element.cabinet.Calendar;
import ru.auto.tests.desktop.element.cabinet.requests.RequestDetailedInfo;
import ru.auto.tests.desktop.element.cabinet.requests.RequestItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetOrdersNewCarsPage extends BasePage {

    @Name("Блок «Заявки на подбор нового автомобиля»")
    @FindBy("//div[@class = 'MatchApplications']")
    VertisElement recallsNewCarsBlock();

    @Name("Активный тумблер в блоке «{{ text }}»")
    @FindBy("//li[contains(., '{{ text }}')]/label[contains(@class, 'Toggle_checked')]")
    VertisElement activeToggle(@Param("text") String text);

    @Name("Неактивный тумблер в блоке «{{ text }}»")
    @FindBy("//li[contains(., '{{ text }}')]/label[not(contains(@class, 'Toggle_checked'))]")
    VertisElement inactiveToggle(@Param("text") String text);

    @Name("Общая информация (количество заявок → стоимость)")
    @FindBy("//div[@class = 'MatchApplicationsFilters__info']")
    VertisElement totalInfo();

    @Name("Список заявок")
    @FindBy("//div[@class = 'MatchApplicationsListing__item']")
    ElementsCollection<RequestItem> requestItemsList();

    @Name("Детальная информация по заявке")
    @FindBy("//div[@class = 'MatchApplicationsDetails MatchApplicationsDetails_visible']")
    RequestDetailedInfo detailedRequestInfo();

    @Name("Кнопка открытия календаря")
    @FindBy("//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();

    @Name("Календарь")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Calendar calendar();

    @Name("Пагинатор")
    @FindBy("//div[contains(@class, 'ListingPagination ')]")
    Pager pager();

    @Step("Получаем заявку с индексом {i}")
    default RequestItem getRecallItem(int i) {
        return requestItemsList().should(hasSize(greaterThan(i))).get(i);
    }
}
