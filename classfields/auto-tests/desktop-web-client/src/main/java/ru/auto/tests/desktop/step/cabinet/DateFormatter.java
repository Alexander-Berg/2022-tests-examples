package ru.auto.tests.desktop.step.cabinet;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 03.05.18
 */
public class DateFormatter {

    public static Date parseOfferPubDate(String pubDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMMM", ruMonthFormatSymbols);
        try {
            return sdf.parse(pubDate);
        } catch (ParseException e) {
            throw new RuntimeException("Дата размещения оффера имеет не подходящий формат. " +
                    "Текущий формат даты " + pubDate, e);
        }

    }

    public static String monthByNumber(int month) {
        return ruMonthFormatSymbols.getMonths()[month];
    }

    private static DateFormatSymbols ruMonthFormatSymbols = new DateFormatSymbols() {

        @Override
        public String[] getMonths() {
            return new String[]{"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        }

    };

    public static String monthInParentCase(int month) {
        return ruMonthInParentCaseSymbols.getMonths()[month];
    }

    public static String today() {
        LocalDate date = LocalDate.now();
        return date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
    }

    public static String weekAgo() {
        LocalDate date = LocalDate.now().minusDays(7);
        return date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
    }

    public static String daysAgo(int i) {
        LocalDate date = LocalDate.now().minusDays(i);
        return date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
    }

    public static String currentMonth() {
        Locale locale = new Locale("ru", "RU");
        LocalDate date = LocalDate.now();
        return date.format(DateTimeFormatter.ofPattern("LLLL", locale));
    }

    public static String firstDayOfMonth() {
        LocalDate date = LocalDate.now();
        return date.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
    }

    private static DateFormatSymbols ruMonthInParentCaseSymbols = new DateFormatSymbols() {
        @Override
        public String[] getMonths() {
            return new String[]{
                    "Января", "Февраля", "Марта", "Апреля", "Мая", "Июня",
                    "Июля", "Августа", "Сентября", "Октября", "Ноября", "Декабря"
            };
        }
    };
}
