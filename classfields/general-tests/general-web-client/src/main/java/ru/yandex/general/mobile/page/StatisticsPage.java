package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface StatisticsPage extends BasePage {

    String EARNED = "Заработали";
    String ACTIVE = "Активные";
    String BANNED = "Отклонённые";
    String EXPIRED = "Завершённые";
    String SOLD = "Проданные";
    String VIEWS = "Просмотры";
    String PHONE_SHOWS = "Показы телефона";
    String CHATS = "Чаты";
    String FAVORITES = "Избранное";

    @Name("Каунтер статистики «{{ value }}»")
    @FindBy("//div[contains(@class, 'StatsCounter_')][contains(., '{{ value }}')]/div[contains(@class, '_value')]")
    VertisElement statisticsCounter(@Param("value") String value);

    @Name("Блок с графиком статистики «{{ value }}»")
    @FindBy("//div[contains(@class, 'StatsMainContent__chart_')][contains(., '{{ value }}')]")
    VertisElement barChart(@Param("value") String value);

    @Name("Блок с графиком статистики «{{ value }}»")
    @FindBy("//div[contains(@class, 'StatsMainContent__chart_')][contains(., '{{ value }}')]" +
            "//div[contains(@class, 'StatsBarChartEmpty__chart')]")
    VertisElement barChartEmpty(@Param("value") String value);
}
