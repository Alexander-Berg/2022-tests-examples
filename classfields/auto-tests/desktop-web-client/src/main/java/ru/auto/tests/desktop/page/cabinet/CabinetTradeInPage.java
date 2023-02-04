package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Pager;
import ru.auto.tests.desktop.element.cabinet.Calendar;
import ru.auto.tests.desktop.element.cabinet.tradein.TradeInItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetTradeInPage extends BasePage {

    @Name("Блок «Заявки на трейд-ин»")
    @FindBy("//div[@class = 'TradeIn']")
    VertisElement tradeInBlock();

    @Name("Вкладка «{{ text }}»")
    @FindBy("//label[.= '{{ text }}']")
    VertisElement tab(@Param("text") String text);

    @Name("Включенный тумблер в «{{ text }}»")
    @FindBy("//li[contains(., '{{ text }}')]/label[contains(@class, 'Toggle_checked')]")
    VertisElement activeToggle(@Param("text") String text);

    @Name("Выключенный тумблер в «{{ text }}»")
    @FindBy("//li[contains(., '{{ text }}')]/label[not(contains(@class, 'Toggle_checked'))]")
    VertisElement inactiveToggle(@Param("text") String text);

    @Name("Список заявок")
    @FindBy("//tr[@class = 'TradeInItem']")
    ElementsCollection<TradeInItem> tradeInItemsList();

    @Name("Кнопка открытия календаря")
    @FindBy("//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();

    @Name("Календарь")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    Calendar calendar();

    @Name("Пагинатор")
    @FindBy("//div[@class = 'ListingPagination']")
    Pager pager();

    @Step("Получаем заявку с индексом {i}")
    default TradeInItem getTradeInItem(int i) {
        return tradeInItemsList().should(hasSize(greaterThan(i))).get(i);
    }
}