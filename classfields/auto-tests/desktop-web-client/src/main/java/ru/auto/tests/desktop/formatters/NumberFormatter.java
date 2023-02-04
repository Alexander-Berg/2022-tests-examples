package ru.auto.tests.desktop.formatters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * User: timondl@yandex-team.ru
 * Date: 19.01.17
 */
public class NumberFormatter {

    private static final String rubSymbol = "\u20BD";
    private static final String kmSymbol = "км";
    private static final String singleOwner = "владелец";
    private static final String manyOwners = "владельца";

    public static String formatPriceWithSpacesAndRub(int price) {
        return formatNumberWithSpacesAndSymbol(price, rubSymbol);
    }

    public static String formatRunWithKm(int run) {
        return formatNumberWithSpacesAndSymbol(run, kmSymbol);
    }

    public static String formatOwnersNumberWithSign(int ownersNumber) {
        String ownerSign = (ownersNumber > 1) ? manyOwners : singleOwner;
        return String.format("%s %s", ownersNumber, ownerSign);
    }

    private static String formatNumberWithSpacesAndSymbol(int number, String symbol) {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

        symbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(symbols);

        return String.format("%s %s", formatter.format(number), symbol);
    }
}
