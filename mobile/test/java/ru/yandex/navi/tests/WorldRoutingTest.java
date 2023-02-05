package ru.yandex.navi.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import io.qameta.allure.junit4.DisplayName;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.RoutePoint;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.tf.RetryRunner;

@SuppressWarnings("NonAsciiCharacters")
@RunWith(RetryRunner.class)
public final class WorldRoutingTest extends BaseTest {
    private final class Country {
        final String id;
        final String name;
        final ArrayList<GeoPoint> points = new ArrayList<>();

        Country(String id, String name) {
            this.id = id;
            this.name = name;
        }

        Country addPoint(String name, double latitude, double longitude) {
            GeoPoint point = new GeoPoint(this.id, name, latitude, longitude);
            points.add(point);
            mapPoints.put(name, point);
            return this;
        }
    }

    private final Map<String, Country> map = new HashMap<>();
    private final Map<String, GeoPoint> mapPoints = new HashMap<>();

    public WorldRoutingTest() {
        addCountry("ABH", "Абхазия")
            .addPoint("Сухум", 43.003852, 41.019151)
            .addPoint("Гагра", 43.280548, 40.265231);

        addCountry("ALA", "Южная Осетия")
            .addPoint("Владикавказ", 43.021150, 44.681960)
            .addPoint("Моздок", 43.735413, 44.653878);

        addCountry("AND", "Андорра")
            .addPoint("Андорра-ла-Велья", 42.506260, 1.521692)
            .addPoint("Аринсаль", 42.572605, 1.482777);

        addCountry("ARM", "Армения")
            .addPoint("Ереван", 40.1535004, 44.4183557)
            .addPoint("Гюмри", 40.785266, 43.841774);

        addCountry("AZE", "Азербайджан")
            .addPoint("Баку", 40.3660919, 49.8350751)
            .addPoint("Сумгаит", 40.595907, 49.669765);

        addCountry("BLR", "Беларусь")
            .addPoint("Минск", 53.902496, 27.561481)
            .addPoint("Витебск", 55.183672, 30.204791);

        addCountry("EST", "Эстония")
            .addPoint("Таллин", 59.437411, 24.745181)
            .addPoint("Тарту", 58.377948, 26.728976);

        addCountry("FIN", "Финляндия")
            .addPoint("Хельсинки", 60.166892, 24.943673)
            .addPoint("Турку", 60.451542, 22.266694);

        addCountry("FRA", "Франция")
            .addPoint("Париж", 48.856663, 2.351556)
            .addPoint("Лион", 45.760199, 4.837715);

        addCountry("GEO", "Грузия")
            .addPoint("Тбилиси", 41.697048, 44.799307)
            .addPoint("Батуми", 41.651102, 41.636267);

        addCountry("GER", "Германия")
            .addPoint("Берлин", 52.519881, 13.407338)
            .addPoint("Мюнхен", 48.137187, 11.575691);

        addCountry("KAZ", "Казахстан")
           .addPoint("Нур-Султан", 51.128207, 71.430411)
           .addPoint("Алматы", 43.238293, 76.945465);

        addCountry("KGZ", "Киргизия")
           .addPoint("Бишкек", 42.876366, 74.603710)
           .addPoint("Ош", 40.517483, 72.805525);

        addCountry("LAT", "Латвия")
           .addPoint("Рига", 56.946840, 24.106075)
           .addPoint("Даугавпилс", 56.942794, 24.141100);

        addCountry("LTU", "Литва")
           .addPoint("Вильнюс", 54.689383, 25.270894)
           .addPoint("Каунас", 54.896775, 23.886635);

        addCountry("MDA", "Молдова")
           .addPoint("Кишинев", 47.024512, 28.832157)
           .addPoint("Тирасполь", 46.835967, 29.606576);

        addCountry("POL", "Польша")
           .addPoint("Варшава", 52.232090, 21.007139)
           .addPoint("Краков", 50.061971, 19.936742);

        addCountry("RUS", "Россия")
           .addPoint("Москва", 55.733969, 37.587093)
           .addPoint("Калининград", 54.710454, 20.512733);

        addCountry("TJK", "Таджикистан")
           .addPoint("Душанбе", 38.576271, 68.779716)
           .addPoint("Худжанд", 40.283611, 69.623962);

        addCountry("TKM", "Туркменистан")
           .addPoint("Ашхабад", 37.935183, 58.378977)
           .addPoint("Туркменабат", 39.054687, 63.580609);

        addCountry("TUR", "Турция")
           .addPoint("Анкара", 39.920756, 32.854049)
           .addPoint("Стамбул", 41.011170, 28.978151);

        addCountry("UKR", "Украина")
           .addPoint("Киев", 50.450441, 30.523550)
           .addPoint("Одесса", 46.484207, 30.731689);

        addCountry("UZB", "Узбекистан")
           .addPoint("Ташкент", 41.311151, 69.279737)
           .addPoint("Самарканд", 39.654515, 66.968847);
    }

    private Country addCountry(String id, String name) {
        Country country = new Country(id, name);
        map.put(id, country);
        return country;
    }

    private GeoPoint getPoint(String name) {
        return mapPoints.get(name);
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableIos.class})
    @DisplayName("[light smoke] Маршруты - весь мир (в пределах страны)")
    @TmsLink("navi-mobile-testing-1482")  // hash: 0x1c4d528d
    public void Маршруты_весь_мир_в_пределах_страны() {
        step("Построить маршрут в пределах стран Россия, Украина, Беларусь, Казахстан", () -> {
            final String[] countries = {"RUS", "UKR", "BLR", "KAZ"};
            for (String country : countries)
                stepBuildRouteInCountry(country);

            expect("Должно быть голосовое и графическое оповещение маневров, обзор маневров. "
                + "Должна быть возможность добавления точки по маршруту. "
                + "Должна быть возможность сбросить маршрут. "
                + "Наличие автозума, если он включен в настройках", () -> {});
        });

        step("Построить маршрут в пределах стран Турция, Азейрбайджан, Армения, Грузия, Киргизия, "
            + "Молдова, Таджикистан, Туркменистан, Узбекистан, Абхазия и Северная Осетия, Польша, "
            + "Литва, Андорра, Латвия, Эстония, Финляндия, Германия, Франция", () -> {
            final String[] countries = {
                "TUR", "AZE", "ARM", "GEO", "KGZ", "MDA", "TJK", "TKM", "UZB", "ABH", "POL", "LTU",
                "AND", "LAT", "EST", "FIN", "GER", "FRA"
            };
            for (String country : countries)
                stepBuildRouteInCountry(country);

            expect("Маршрут строится", () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableIos.class})
    @DisplayName("[light smoke] Маршруты - весь мир (Построение из Беларуси)")
    @TmsLink("navi-mobile-testing-1492")  // hash: 0xc3c82eac
    public void Маршруты_весь_мир_Построение_из_Беларуси() {
        step("Построить маршрут из Беларуси в Россию", () -> {
            buildRoute(getPoint("Минск"), getPoint("Москва"), null, false);
            expect("Маршрут строится", () -> {});
        });

        step("Построить маршрут из Беларуси, через Украину в Казахстан", () -> {
            buildRoute(getPoint("Минск"), getPoint("Алматы"),
                Collections.singletonList(RoutePoint.way(getPoint("Одесса"))), false);
            expect("Маршрут строится", () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableIos.class})
    @DisplayName("[light smoke] Маршруты - весь мир (Построение из России)")
    @TmsLink("navi-mobile-testing-1493")  // hash: 0x377fcc11
    public void Маршруты_весь_мир_Построение_из_России() {
        step("Построить маршрут из России в Францию, Германию, Финляндию, Латвию, Эстонию", () -> {
            final String[] countries = {"FRA", "GER", "FIN", "LAT", "EST"};
            for (String country : countries)
                stepBuildRouteToCountry(country);
            expect("Маршрут успешно построился. "
                + "Есть информация о пересечении границы", () -> {});
        });
    }

    @SuppressWarnings("NonAsciiCharacters")
    @Test
    @Category({UnstableIos.class})
    @DisplayName("[light smoke] Маршруты - весь мир (Калининград)")
    @TmsLink("navi-mobile-testing-1494")  // hash: 0x0b4a91a1
    public void Маршруты_весь_мир_Калининград() {
        step("Построить маршрут в Калининград", () -> {
            buildRouteAndGo(getPoint("Калининград"));
            expect("Маршрут строится", () -> {});
        });
    }

    @Step("Построить маршрут в пределах {country}")
    private void stepBuildRouteInCountry(String country) {
        ArrayList<GeoPoint> points = map.get(country).points;
        assert points.size() >= 2;
        buildRoute(points.get(0), points.get(1), null, false);
    }

    @Step("Построить маршрут из России в {country}")
    private void stepBuildRouteToCountry(String country) {
        final GeoPoint to = map.get(country).points.get(0);
        buildRouteAndGo(to);
    }

    @Test
    @Category({UnstableIos.class})
    @Issue("MOBNAVI-22062")
    @Ignore("MOBNAVI-23917")
    @TmsLink("navigator-1109")  // hash: 0xd18f8fc7
    public void routeBySearch() {
        String[] countries = {"UKR", "BLR", "KAZ", "LAT", "EST", "FRA", "GER", "FIN"};
        for (String id : countries) {
            final Country country = map.get(id);
            final String city = country.points.get(0).name;
            mapScreen.buildRouteBySearchAndGo(country.name + " " + city);
        }
    }
}
