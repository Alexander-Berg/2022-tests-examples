package ru.auto.tests.desktop.step;

import com.google.gson.Gson;
import io.qameta.allure.Step;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.Cookie;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.desktop.DesktopConfig;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;

import static java.lang.String.format;
import static java.net.URLDecoder.decode;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * Created by vicdev on 23.08.17.
 */
public class CookieSteps extends WebDriverSteps {

    public static final String AUTORU_SID = "autoru_sid";
    public static final String AUTORU_UID = "autoruuid";
    public static final String COOKIE_NAME_NOTE_FAV_INFO = "sale-note-fav-info";
    public static final String GIDS = "gids";
    public static final String GRADIUS = "gradius";
    public static final String BACK_ON_SALE_PAGE_COOKIE = "is_showing_back_on_sale_placeholder";
    public static final String EXP_FLAGS = "exp_flags";
    public static final String CALLS_PROMOTE_CLOSED = "calls-promote-closed";
    public static final String CLOSED = "closed";
    private final static String MOCKRITSA_HOST_COOKIE = "mockritsa_mock_host";
    private final static String MOCKRITSA_PORT_COOKIE = "mockritsa_imposter";
    public final static String GARAGE_BANNER_CLOSED = "garage_banner_closed";
    public final static String BRANCH = "_branch";
    public final static String AUCTION_TOUR_STEP = "auction_tour_step";
    public final static String SHOWN = "SHOWN";
    public static final String RESELLER_PUBLIC_PROFILE_POPUP_SHOWN = "reseller-public-profile-popup-shown";
    public static final String RESELLER_PUBLIC_PROFILE_LK_BANNER_SHOWN = "reseller_public_profile_lk_banner_shown";
    public final static String IS_SHOWING_ONBOARDING_ARCHIVE = "is_showing_onboarding_archive";
    public final static String IS_SHOWING_ONBOARDING_MASS_ACTION = "is_showing_onboarding_mass_action";
    public final static String IS_SHOWING_ONBOARDING_FILTERS = "is_showing_onboarding_filters";
    public final static String IS_SHOWING_ONBOARDING_AUTOBIDDER = "is_showing_onboarding_autobidder";
    public final static String IS_SHOWING_ONBOARDING_PRICE_REPORT = "is_showing_onboarding_price_report";
    public final static String IS_SHOWING_BACK_ON_SALE_PLACEHOLDER = "is_showing_back_on_sale_placeholder";
    public final static String SAFE_DEAL_SELLER_ONBOARDING_PROMO = "safe_deal_seller_onboarding_promo";
    public final static String GREAT_DEAL_POPUP = "great-deal-popup";
    public final static String AUTORU_EXCLUSIVE_POPUP = "autoru-exclusive-popup";
    public final static String NOTIFICATION_YANDEX_AUTH_SUGGEST_SEEN = "notification_yandex_auth_suggest_seen";
    public final static String DATE_IN_PAST = "2022-01-18T18:47:35+03:00";
    public final static String PREFER_BEM_POFFER = "prefer_bem_poffer";
    public final static String FORCE_DISABLE_TRUST = "force_disable_trust";
    public final static String FORCE_IGNORE_TRUST_EXP_RESULT = "force_ignore_trust_exp_result";

    public static final String EXP_AUTORUFRONT_20574 = "AUTORUFRONT-20574_c2b_banner";
    public static final String EXP_AUTORUFRONT_19219 = "AUTORUFRONT-19219_new_lk_and_vas_block_design";
    public static final String EXP_AUTORUFRONT_18214 = "AUTORUFRONT-18214_score";
    public static final String EXP_AUTORUFRONT_17392 = "AUTORUFRONT-17392_exp_promo_banner";
    public static final String EXP_AUTORUFRONT_17695 = "AUTORUFRONT-17695_sales-assistant";
    public static final String EXP_AUTORUFRONT_21494 = "AUTORUFRONT-21494_mobile_poffer";
    public static final String EXP_AUTORUFRONT_22049 = "AUTORUFRONT-22049_show-exchange-popup";
    public static final String PROMO_NEW_CARS_FIRST_LETTER_TYPED = "promo_new_cars_first_letter_typed_cookie";

    @Inject
    public DesktopConfig config;

    @Step("Ставим региональную куку {geo} через яндекс")
    public void setGeoCookie(String geo) {
        onYandexPage();
        setCookie(config.getYandexGeoCookieName(), geo, "yandex.ru");
    }

    @Step("Ставим куку: name={cookieName}, value={cookieValue}, domain={cookieDomain}")
    public void setCookie(String cookieName, String cookieValue, String cookieDomain) {
        Cookie cookie = getDriver().manage().getCookieNamed(cookieName);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.YEAR, 10);
        Cookie newCookie = new Cookie(cookieName, cookieValue, cookieDomain, "/", calendar.getTime());
        if (cookie != null) {
            getDriver().manage().deleteCookieNamed(cookieName);
        }
        getDriver().manage().addCookie(newCookie);
    }

    public void setCookieForBaseDomain(String cookieName, String cookieValue) {
        setCookie(cookieName, cookieValue, format(".%s", config.getBaseDomain()));
    }

    @Step("Удаляем куку: name={cookieName}")
    public void deleteCookie(String cookieName) {
        Cookie cookie = getDriver().manage().getCookieNamed(cookieName);
        if (cookie != null) {
            getDriver().manage().deleteCookieNamed(cookieName);
        }
    }

    @Step("Не должны видеть куку «{name}»")
    public void shouldNotSeeCookie(String name) {
        await().pollInterval(1, SECONDS).atMost(5, SECONDS)
                .untilAsserted(() -> assertThat(String.format("Не должно быть куки «%s»", name),
                        getDriver().manage().getCookieNamed(name), nullValue()));
    }

    @Step("Должны видеть куку «{name}»")
    public void shouldSeeCookie(String name) {
        await().pollInterval(1, SECONDS).atMost(5, SECONDS)
                .untilAsserted(() -> Assertions.assertThat(getDriver().manage().getCookieNamed(name))
                        .describedAs(String.format("Должны видеть куку «%s»", name)).isNotNull());
    }

    @Step("Должны видеть куку «{name}» со значением «{value}»")
    public void shouldSeeCookieWithValue(String name, String value) {
        await().pollInterval(1, SECONDS).atMost(5, SECONDS).ignoreExceptions()
                .untilAsserted(() -> Assertions.assertThat(getDriver().manage().getCookieNamed(name).getValue())
                        .describedAs(String.format("Должны видеть куку «%s» со значением «%s»",
                                name, value)).isEqualTo(value));
    }

    @Step("Выставляем регион «{region}» через куку")
    public void setRegion(String region) {
        clearCookie(GIDS);
        setCookie(GIDS, region, format(".%s", config.getBaseDomain()));
    }

    public void setExpFlags(String... expFlags) {
        Gson gson = new Gson();
        String expFlagsSting = gson.toJson(expFlags);

        setCookieForBaseDomain(EXP_FLAGS, expFlagsSting);
    }

    @Step("Выставляем куки мокрицы, port = «{port}»")
    public void setMockritsaPortAndHostCookies(String port) {
        setCookieForBaseDomain(MOCKRITSA_HOST_COOKIE, config.getMockritsaMockHost());
        setCookieForBaseDomain(MOCKRITSA_PORT_COOKIE, port);
    }

    public String getSessionId() {
        String sessionId = "";

        try {
            sessionId = decode(getCookieBy("autoru_sid").getValue(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return sessionId;
    }

}
