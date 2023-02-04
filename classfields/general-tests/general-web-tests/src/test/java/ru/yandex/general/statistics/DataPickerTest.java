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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.DataPickerPopup.MONTH;
import static ru.yandex.general.element.DataPickerPopup.WEEK;
import static ru.yandex.general.utils.Utils.getCalendar;
import static ru.yandex.general.utils.Utils.getCurrentDate;
import static ru.yandex.general.utils.Utils.getDateEarlier;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(STATISTICS)
@Feature("Дата пикер")
@DisplayName("Дата пикер")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class DataPickerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(STATS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Дефолтный диапазон дат в кнопке открытия дата пикера")
    public void shouldSeeDefaultDateRangeInSelectorButton() {
        basePageSteps.onStatisticsPage().dateSelector().should(hasText(
                formatDateSelectorText(getDateEarlier(6), getCurrentDate())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Дефолтный диапазон выбранных дней в попапе дата пикера")
    public void shouldSeeDefaultDateRangeDaysCountInDataPickerPopup() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup().selectedDays().should(hasSize(7));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Дефолтный диапазон выбранных дней, сегодня - последний выбранный день")
    public void shouldSeeDefaultDateRangeTodayEnd() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup()
                .month(formatMonth(getCurrentDate()))
                .endDay().should(hasText(formatDay(getCurrentDate())));

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Дефолтный диапазон выбранных дней, 7 дней назад - первый выбранный день")
    public void shouldSeeDefaultDateRangeStart() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup()
                .month(formatMonth(getDateEarlier(6)))
                .startDay().should(hasText(formatDay(getDateEarlier(6))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Стрелка переключения месяца вперед задизейблена")
    public void shouldSeeNextMonthArrowDisabled() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup().arrowRight().should(hasAttribute("disabled", "true"));

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаем месяцы стрелкой влево, отображается 2 прошлых месяца")
    public void shouldSeeLeftArrowChangeMonths() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().arrowLeft().click();

        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getDateMonthsAgo(2))).should(isDisplayed());
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getDateMonthsAgo(1))).should(isDisplayed());
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getCurrentDate())).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключаем месяцы стрелкой влевой, затем стрелкой вправо, отображаются текущий и прошлый месяцы")
    public void shouldSeeRightArrowChangeMonths() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().arrowLeft().click();
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getDateMonthsAgo(2))).waitUntil(isDisplayed());
        basePageSteps.onStatisticsPage().dataPickerPopup().arrowRight().click();

        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getDateMonthsAgo(2))).should(not(isDisplayed()));
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getDateMonthsAgo(1))).should(isDisplayed());
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(getCurrentDate())).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кликаем на день 2 недели раньше - меняется день начального диапазона, выбран один день")
    public void shouldChangeStartDay() {
        Date fromDate = getDateEarlier(14);

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);

        basePageSteps.onStatisticsPage().dataPickerPopup().selectedDays().should(hasSize(1));
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(fromDate))
                .startDay().should(hasText(formatDay(fromDate)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап дата пикера закрывается при выборе диапазона")
    public void shouldSeeClosePopupAfterChangeRange() {
        Date fromDate = getDateEarlier(14);
        Date toDate = getCurrentDate();

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(toDate);

        basePageSteps.onStatisticsPage().dataPickerPopup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем диапазон дат, при следующем открытии он сохраняется")
    public void shouldSeeChangedDateRange() {
        Date fromDate = getDateEarlier(21);
        Date toDate = getDateEarlier(14);

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(toDate);
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();


        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(fromDate)).startDay()
                .should(hasText(formatDay(fromDate)));
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(toDate)).endDay()
                .should(hasText(formatDay(toDate)));
        basePageSteps.onStatisticsPage().dataPickerPopup().selectedDays().should(hasSize(8));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем период «Месяц» в дата пикере")
    public void shouldSetMonthInDataPicker() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(MONTH).waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup().should(not(isDisplayed()));
        basePageSteps.onStatisticsPage().dateSelector().should(hasText(
                formatDateSelectorText(getDateEarlier(29), getCurrentDate())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем период «Неделя» в дата пикере")
    public void shouldSetWeekInDataPicker() {
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(MONTH).waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(WEEK).waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup().should(not(isDisplayed()));
        basePageSteps.onStatisticsPage().dateSelector().should(hasText(
                formatDateSelectorText(getDateEarlier(6), getCurrentDate())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем период «Месяц» в дата пикере, при следующем открытии он сохраняется")
    public void shouldSeeChangedRangeAfterSetMonthInDataPicker() {
        Date fromDate = getDateEarlier(29);
        Date toDate = getCurrentDate();

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().periodChip(MONTH).waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(fromDate)).startDay()
                .should(hasText(formatDay(fromDate)));
        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(toDate)).endDay()
                .should(hasText(formatDay(toDate)));
        basePageSteps.onStatisticsPage().dataPickerPopup().selectedDays().should(hasSize(30));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Максимальный диапазон - месяц, следующий день задизейблен")
    public void shouldSeeMaximumMonthInRange() {
        Date fromDate = getDateTwoMonthsAgo(getRandomIntInRange(1, 15));
        Date toDate = getDate29DaysAfter(fromDate);

        basePageSteps.onStatisticsPage().dateSelector().waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().dataPickerPopup().arrowLeft().click();
        basePageSteps.onStatisticsPage().dataPickerPopup().clickDay(fromDate);

        basePageSteps.onStatisticsPage().dataPickerPopup().month(formatMonth(toDate))
                .day(String.valueOf(toDate.getDate() + 1)).should(hasAttribute("aria-disabled", "true"));
    }

    private static String formatDate(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("d MMMM yyyy", locale);

        return formatter.format(date).toLowerCase();
    }

    private static String formatDay(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("d", locale);

        return formatter.format(date).toLowerCase();
    }

    private static String formatMonth(Date date) {
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM", locale);

        return formatter.format(date).toLowerCase();
    }

    private static Date getDateMonthsAgo(int monthsAgo) {
        Calendar c = getCalendar();
        c.add(Calendar.MONTH, -monthsAgo);

        return c.getTime();
    }

    private String formatDateSelectorText(Date fromDate, Date toDate) {
        String selectorDate;
        Locale locale = new Locale("ru");
        SimpleDateFormat formatter = new SimpleDateFormat("d MMMM", locale);

        if (fromDate.getMonth() == toDate.getMonth())
            selectorDate = format("%s – %s", formatDay(fromDate), formatDate(toDate));
        else selectorDate = format("%s – %s", formatter.format(fromDate).toLowerCase(), formatDate(toDate));

        return selectorDate;
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
