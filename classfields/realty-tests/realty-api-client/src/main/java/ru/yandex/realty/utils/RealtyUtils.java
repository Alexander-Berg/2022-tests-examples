package ru.yandex.realty.utils;

import com.google.gson.GsonBuilder;
import ru.auto.test.api.realty.user.create.Request;
import ru.auto.test.api.realty.user.create.UserReq;
import ru.auto.tests.commons.util.Utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static ru.auto.tests.commons.util.Utils.getRandomBoolean;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

/**
 * Created by vicdev on 17.04.17.
 */
public class RealtyUtils {

    private RealtyUtils() {
    }

    public static UserReq getRandomUserRequestBody(String uid, long type) {
        return new UserReq().withRequest(getRequest(Request.class)).withLogin(uid)
                .withEmail(Utils.getRandomEmail())
                .withLicenseAgreement(getRandomBoolean()).withName(getRandomString())
                .withOrganization(getRandomString()).withOgrn(getStaticOgrn())
                .withAgencyId(getRandomString())
                .withType(type)
                .withTelephones(newArrayList(Utils.getRandomPhone(), Utils.getRandomPhone()))
                .withCallCenter(getRandomBoolean()).withUrl(getRandomString()).withPhotoUrl(getRandomString());
    }

    public static <T> T getRequest(Class<T> tClass) {
        return getObjectFromJson(tClass, "request.json");
    }

    public static <T> T getObjectFromJson(Class<T> tClass, String path) {
        return new GsonBuilder().create().fromJson(getResourceAsString(path), tClass);
    }

    public static int getRandomPrice() {
        return (int) (Math.random() * 9000000 + 1000000);
    }

    public static String getRandomApartment() {
        return String.valueOf(nextInt(1, 100));
    }

    public static String reformatOfferCreateDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Для агентства ОГРН/ОГРНИП 15 любых чисел
     *
     * @return
     */
    public static String getStaticOgrn() {
        return "1117746307140";
    }
}
