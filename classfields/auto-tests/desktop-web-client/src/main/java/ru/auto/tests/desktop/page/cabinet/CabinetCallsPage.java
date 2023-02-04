package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.element.Menu;
import ru.auto.tests.desktop.element.cabinet.calls.Call;
import ru.auto.tests.desktop.element.cabinet.calls.Filters;
import ru.auto.tests.desktop.element.cabinet.calls.Stats;
import ru.auto.tests.desktop.element.cabinet.calls.Tabs;
import ru.auto.tests.desktop.element.cabinet.manager.ComplaintPopup;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.05.18
 */
public interface CabinetCallsPage extends BasePage, WithPager {

    @Name("Содержимое страницы")
    @FindBy("//div[contains(@class, 'CallTracking')]")
    VertisElement content();

    @Name("Заголовок")
    @FindBy("//div[contains(@class, 'CallTracking__title')]")
    VertisElement title();

    @Name("Вкладки")
    @FindBy("//div[contains(@class, 'ServiceNavigation')]")
    Tabs tabs();

    @Name("Фильтры")
    @FindBy("//div[contains(@class, 'CallsFilters')]")
    Filters filters();

    @Name("Статистика")
    @FindBy("//div[contains(@class, 'CallsTotalStats')]")
    Stats stats();

    @Name("Список звонков")
    @FindBy("//div[contains(@class, 'CallsListing__item')]")
    ElementsCollection<Call> callsList();

    @Name("Поп-ап жалоб")
    @FindBy(".//div[contains(@class, 'Modal_visible')]")
    ComplaintPopup complaintPopup();

    @Name("Поп-ап-меню")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Menu menu();

    @Step("Получаем звонок с индексом {i}")
    default Call getCall(int i) {
        return callsList().should(hasSize(greaterThan(i))).get(i);
    }
}
