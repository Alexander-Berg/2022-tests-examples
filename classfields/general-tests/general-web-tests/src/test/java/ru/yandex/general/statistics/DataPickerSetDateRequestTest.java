package ru.yandex.general.statistics;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static ru.yandex.general.beans.ajaxRequests.GetUserActionsStatistics.getUserActionsStatistics;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.DataPickerPopup.MONTH;
import static ru.yandex.general.element.DataPickerPopup.WEEK;
import static ru.yandex.general.step.AjaxProxySteps.GET_USER_ACTIONS_STATISTICS;
import static ru.yandex.general.utils.Utils.getCalendar;
import static ru.yandex.general.utils.Utils.getCurrentDate;
import static ru.yandex.general.utils.Utils.getDateEarlier;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(STATISTICS)
@Feature("Дата пикер")
@DisplayName("Дата пикер, проверка запросов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class DataPickerSetDateRequestTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(STATS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос. Выбираем период «Месяц» в дата пикере")
    public void shouldSetMonthInDataPicker() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(MONTH).waitUntil(isDisplayed()).click();

        ajaxProxySteps.setAjaxHandler(GET_USER_ACTIONS_STATISTICS).withRequestText(
                getUserActionsStatistics().setFromDate(formatDate(getDateEarlier(29)))
                        .setToDate(formatDate(getCurrentDate()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос. Выбираем период «Неделя» в дата пикере")
    public void shouldSetWeekInDataPicker() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(MONTH).waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(WEEK).waitUntil(isDisplayed()).click();


        ajaxProxySteps.setAjaxHandler(GET_USER_ACTIONS_STATISTICS).withRequestText(
                getUserActionsStatistics().setFromDate(formatDate(getDateEarlier(6)))
                        .setToDate(formatDate(getCurrentDate()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос. Выбираем в дата пикере диапазон 2 недели до сегодняшней даты")
    public void shouldSetRangeTwoWeeksTillToday() {
        Date fromDate = getDateEarlier(14);
        Date toDate = getCurrentDate();

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(toDate);

        ajaxProxySteps.setAjaxHandler(GET_USER_ACTIONS_STATISTICS).withRequestText(
                getUserActionsStatistics().setFromDate(formatDate(fromDate))
                        .setToDate(formatDate(toDate))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос. Выбираем в дата пикере диапазон в 2 дня")
    public void shouldSetRangeTwoDays() {
        Date fromDate = getDateEarlier(5);
        Date toDate = getDateEarlier(4);

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(toDate);

        ajaxProxySteps.setAjaxHandler(GET_USER_ACTIONS_STATISTICS).withRequestText(
                getUserActionsStatistics().setFromDate(formatDate(fromDate))
                        .setToDate(formatDate(toDate))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос. Выбираем в дата пикере диапазон 2 месяца назад в пределах одного месяца")
    public void shouldSetRangeTwoMonthsBefore() {
        Date fromDate = getDateTwoMonthsAgo(getRandomIntInRange(1, 15));
        Date toDate = getDateTwoMonthsAgo(getRandomIntInRange(16, 28));

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().arrowLeft().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(toDate);

        ajaxProxySteps.setAjaxHandler(GET_USER_ACTIONS_STATISTICS).withRequestText(
                getUserActionsStatistics().setFromDate(formatDate(fromDate))
                        .setToDate(formatDate(toDate))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверяем запрос. Выбираем в дата пикере диапазон 2 месяца назад величиной в месяц")
    public void shouldSetMonthRangeTwoMonthsBefore() {
        Date fromDate = getDateTwoMonthsAgo(getRandomIntInRange(5, 15));
        Date toDate = getDate29DaysAfter(fromDate);

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().arrowLeft().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(toDate);

        ajaxProxySteps.setAjaxHandler(GET_USER_ACTIONS_STATISTICS).withRequestText(
                getUserActionsStatistics().setFromDate(formatDate(fromDate))
                        .setToDate(formatDate(toDate))).shouldExist();
    }

    private static String formatDate(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", locale);

        return formatter.format(date).toLowerCase();
    }

    private static Date getDateTwoMonthsAgo(int day) {
        Calendar c = getCalendar();
        c.add(Calendar.MONTH, -2);
        c.set(Calendar.DAY_OF_MONTH, day);

        return c.getTime();
    }

    private static Date getDate29DaysAfter(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 29);

        return c.getTime();
    }

}
