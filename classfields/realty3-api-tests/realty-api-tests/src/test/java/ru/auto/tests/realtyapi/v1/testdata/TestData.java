package ru.auto.tests.realtyapi.v1.testdata;

import io.restassured.builder.RequestSpecBuilder;
import org.joda.time.Duration;
import ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse;
import ru.auto.tests.realtyapi.v1.model.VosUserModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.util.Utils.getRandomShortLong;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.v1.api.MoneyApi.PartnerAggregatedSpentOper.LEVEL_QUERY;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.COMMERCIAL;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.GARAGE;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.HOUSE;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.LOT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.ROOMS;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.RENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.END;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.START;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.getNearFutureTime;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.getNearPastTime;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.getFurtherPastTime;

public class TestData {

    public static final String ME = "me";
    public static final int INVALID_PAGE = -1;

    public static List<Boolean> trueFalse() {
        return Arrays.asList(true, false);
    }

    public static Object[] invalidPhones() {
        return new String[]{
                valueOf(getRandomShortLong()),
                EMPTY,
                getRandomString(),
                getRandomPhone()
        };
    }

    public static Object[] invalidXAuthorization() {
        return new String[]{
                valueOf(getRandomShortLong()),
                getRandomString(),
                getRandomPhone()
        };
    }

    public static Object[] trueCreateRedirectGeoId() {
        return new String[]{
                "10995",
                "116900",
                "121158",
                "99298"
        };
    }

    public static List<String> validAddresses() {
        return Arrays.asList(
                "Россия, Санкт-Петербург, Невский проспект, 128",
                "Россия, Приморский край, Владивосток, проспект Острякова, 6",
                "Россия, Приморский край, Владивосток, Минеральная улица, 9А",
                "Россия, Приморский край, Владивосток, улица Державина, 21",
                "Россия, Москва, улица Малая Дмитровка, 20",
                "Россия, Приморский край, Владивосток, Некрасовская улица, 53",
                "Россия, Приморский край, Владивосток, улица Котельникова, 24",
                "Россия, Санкт-Петербург, Петергоф, Санкт-Петербургский проспект, 60",
                "Россия, Приморский край, Владивосток, СНТ Зелёный Угол, 122"
        );
    }

    public static Long[] validRgids() {
        return new Long[]{
                587795L, // Moscow
                417899L, //  Saint P.
                417941L, // Kronshtadt
                353118L, // Krasnodarskiy krai
                573359L, // Vologda
                2318L, // okrug Khimki
                593545L, // Khimki
                406766L, // Kaliningrad
                585430L, // Vladivostok
                16898142L, // Ulan-Ude
                552970L // Omsk
        };
    }

    public static List<String> defaultStatAggregation() {
        return Arrays.asList(
                "day",
                "all"
        );
    }

    public static Object[] defaultUserType() {
        return new Integer[]{
                0, // owner
                1, // agent
                2, // developer
                3, // agency
                7 // verifier
        };
    }

    public static List<String> defaultOffers() {
        return Arrays.stream(OfferType.values()).map(OfferType::value).collect(toList());

    }

    public static Object[] defaultInvalidOffers() {
        return Arrays.stream(InvalidOffer.values()).map(InvalidOffer::value).toArray();
    }

    public static List<RealtyResponseOfferResponse.OfferCategoryEnum> defaultOfferCategory() {
        return Arrays.asList(ROOMS, APARTMENT, HOUSE, LOT, COMMERCIAL, GARAGE);
    }

    public static List<RealtyResponseOfferResponse.OfferTypeEnum> defaultOfferType() {
        return Arrays.asList(SELL, RENT);
    }

    public static List<String> defaultNumberOfRooms() {
        return Arrays.asList(
                "1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "PLUS_7",
                "STUDIO",
                "OPEN_PLAN");

    }

    public static Object[] defaultPaymentType() {
        return Arrays.stream(VosUserModel.PaymentTypeEnum.values()).map(VosUserModel.PaymentTypeEnum::getValue).toArray();
    }

    public static class GetDate {
        static final String START = "startTime";
        static final String END = "endTime";

        public static String getNearPastTime() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            Date date = new Date(System.currentTimeMillis() - Duration.standardDays(3).getMillis());
            return formatter.format(date);
        }

        public static String getFurtherPastTime() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            Date date = new Date(System.currentTimeMillis() - Duration.standardDays(7).getMillis());
            return formatter.format(date);
        }

        public static String getNearFutureTime() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            Date date = new Date(System.currentTimeMillis() + Integer.MAX_VALUE);
            return formatter.format(date);
        }

        public static String getFurtherFutureTime() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            long next = (long) Integer.MAX_VALUE * 2;
            Date date = new Date(System.currentTimeMillis() + next);
            return formatter.format(date);
        }
    }

    public static Collection<Consumer<RequestSpecBuilder>> getParametersForAggregatedOfferStatComparison() {
        // С одной стороны, списания создаются руками, так что автогенерить эти значения нельзя.
        // С другой, если запрашивать данные за все время, их настолько много, что парсер жсона работает долго.
        String somewhatBeforeLatest = "2021-01-27T00:00:00.000+03:00";
        String somewhatAfterEarliest = "2020-09-28T00:00:00.000+03:00";

        // здесь тестируем только закрытые интервалы, так как открытые интервалы либо не дают гарантий непустого ответа
        // либо не дают гарантий размера интервала
        return defaultStatAggregation().stream().flatMap(stat -> Stream.<Consumer<RequestSpecBuilder>>of(

                req -> req.addQueryParam(START, somewhatBeforeLatest).addQueryParam(END, getNearFutureTime()).addQueryParam(LEVEL_QUERY, stat)
        )).collect(toList());
    }

    public static Collection<Consumer<RequestSpecBuilder>> getParametersForPartnerEmpty() {
        List<Consumer<RequestSpecBuilder>> parameters = new ArrayList<>();

        parameters.add(req -> req.addQueryParam(START, getNearFutureTime()).addQueryParam(END, getNearFutureTime()));

        return parameters;
    }

    public static Collection<Consumer<RequestSpecBuilder>> getParametersForUserEmpty() {
        List<Consumer<RequestSpecBuilder>> parameters = new ArrayList<>();

        parameters.add(req -> req.addQueryParam(START, getNearPastTime()).addQueryParam(END, getNearPastTime()));
        parameters.add(req -> req.addQueryParam(START, getNearFutureTime()).addQueryParam(END, getNearFutureTime()));

        return parameters;
    }

    public static List<String> getDrafts() {
        return Arrays.stream(Drafts.values()).map(Drafts::value).collect(toList());
    }

    private static final Map<String, String> dealStatus = new HashMap<String, String>() {
        {
            put("REASSIGNMENT", "Переуступка");
            put("PRIMARY_SALE", "Первичная продажа");
            put("SALE", "Свободная продажа");
        }
    };

    public static Map<String, String> getDealStatus() {
        return dealStatus;
    }
}

