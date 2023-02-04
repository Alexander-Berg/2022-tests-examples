package ru.yandex.general.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.internal.http.URIBuilder;
import ru.yandex.general.beans.card.Statistics;
import ru.yandex.general.beans.card.StatisticsGraphItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static ru.yandex.general.beans.card.Statistics.statistics;
import static ru.yandex.general.beans.card.StatisticsGraphItem.statisticsGraphItem;

public class Utils {

    public static int getRandomIntInRange(int min, int max) {
        Random random = new Random();

        return random.nextInt(max - min) + min;
    }

    public static String getRandomOfferId() {
        int min = 1000000;
        int max = 9999999;

        return String.valueOf(getRandomIntInRange(min, max));
    }

    public static String getISOFormatedDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return formatter.format(date);
    }

    public static JsonObject getStatisticsGraph(int daysCount) {
        JsonArray records = new JsonArray();
        JsonObject statisticsGraph = new JsonObject();

        getLastDates(daysCount).forEach(date -> {
            StatisticsGraphItem graphItem = statisticsGraphItem()
                    .setDate(date)
                    .setHighlighted(false)
                    .setStatistics(getRandomDailyStatistics());

            records.add(new Gson().toJsonTree(graphItem).getAsJsonObject());
        });

        statisticsGraph.add("records", records);
        return statisticsGraph;
    }

    public static Statistics getRandomDailyStatistics() {
        return statistics()
                .setViewsCount(getRandomStatsCount())
                .setFavoritesCount(getRandomStatsCount())
                .setContactsCount(getRandomStatsCount());
    }

    public static Statistics getEmptyDailyStatistics() {
        return statistics()
                .setViewsCount(0)
                .setFavoritesCount(0)
                .setContactsCount(0);
    }

    public static List<String> getLastDates(int daysCount) {
        List<String> lastWeekdates = new ArrayList<>();

        Calendar c = getCalendar();
        for (int i = 0; i < daysCount; i++) {
            lastWeekdates.add(formatDate(c.getTime()));
            c.add(Calendar.DATE, -1);
        }
        Collections.reverse(lastWeekdates);

        return lastWeekdates;
    }

    public static Date getCurrentDate() {
        return getCalendar().getTime();
    }

    public static Date getDateEarlier(int daysBefore) {
        Calendar c = getCalendar();
        c.add(Calendar.DATE, -daysBefore);
        return c.getTime();
    }

    public static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    public static Calendar getCalendar() {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    private static int getRandomStatsCount() {
        int min = 0;
        int max = 100;
        Random random = new Random();

        return random.nextInt(max - min);
    }

    public static String removeExplitRussiaRegionParam(String url) {
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(new URI(url), true, encoderConfig());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (uriBuilder.hasQueryParam("explicit_russia_region")) {
            try {
                uriBuilder.removeQueryParam("explicit_russia_region");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return uriBuilder.toString();
    }

    public static String formatPhone(String phone) {
        return phone.replace("(", "").replace(")", "")
                .replaceFirst("([+]\\d{1})(\\d{3})(\\d{3})(\\d{2})(\\d{2})", "$1 $2 $3-$4-$5");
    }

    public static String formatPrice(long price) {
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setGroupingSeparator(' ');

        decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);

        return decimalFormat.format(price) + " ₽";
    }

    public static String formatPrice(String price) {
        return formatPrice(Long.valueOf(price));
    }

}
