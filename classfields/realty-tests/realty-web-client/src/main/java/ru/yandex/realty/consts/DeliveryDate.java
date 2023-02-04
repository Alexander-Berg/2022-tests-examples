package ru.yandex.realty.consts;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

import static java.lang.String.format;

public class DeliveryDate {

    private static final int MONTHS_IN_QUARTER = 3;
    private static final int HALF_YEAR_IN_QUARTERS = 2;
    public static final LocalDate NOW = LocalDate.now();

    //TODO "Привести к системе, чтобы вспроверки шли в одном файле. Уйти от разных файлов для ЧПУ и без"
    public static String getRelativeUrlParam(int plusQuarter) {
        LocalDate deliveryDate = NOW.plusMonths(MONTHS_IN_QUARTER * plusQuarter);
        return format("%d_%d", deliveryDate.get(IsoFields.QUARTER_OF_YEAR), deliveryDate.getYear());
    }

    public static String getItemForPlusQuarter(int plusQuarter) {
        LocalDate deliveryDate = NOW.plusMonths(MONTHS_IN_QUARTER * plusQuarter);
        return format("До %d квартала %d", deliveryDate.get(IsoFields.QUARTER_OF_YEAR), deliveryDate.getYear());
    }

    public enum RelativeDate {
        HALF_YEAR("Полгода"),
        ONE_YEAR("1 год"),
        ONE_AND_HALF_YEARS("1,5 года"),
        TWO_YEARS("2 года"),
        TWO_AND_HALF_YEARS("2,5 года"),
        THREE_YEARS("3 года");

        String relativeDate;

        RelativeDate(String relativeDate) {
            this.relativeDate = relativeDate;
        }

        public String getRelativeDate() {
            return relativeDate;
        }

        public int getInQuarters() {
            return HALF_YEAR_IN_QUARTERS + ordinal() * HALF_YEAR_IN_QUARTERS;
        }
    }
}
