package ru.auto.tests.desktop;

import org.aeonbits.owner.Config;

import java.net.URI;
import java.util.List;

@Config.Sources("classpath:testing.properties")
public interface DesktopConfig extends Config {

    Integer MAX_PAGES = 99;

    Integer LISTING_TOP_SALES_CNT = 3;

    @Key("mockritsa.api.url")
    @DefaultValue("http://mockritsa-autoru-api.vrts-slb.test.vertis.yandex.net:80")
    String getMockritsaApiUrl();

    @Key("mockritsa.mock.host")
    @DefaultValue("mockritsa-autoru-mock.vrts-slb.test.vertis.yandex.net")
    String getMockritsaMockHost();

    @Key("baseCookies")
    List<String> getBaseCookies();

    @Key("desktop.baseDomain")
    @DefaultValue("test.avto.ru")
    String getBaseDomain();

    @Key("desktop.autoruDomain")
    @DefaultValue("auto.ru")
    String getAutoruDomain();

    @Key("desktop.testDomain")
    @DefaultValue("test.avto.ru")
    String getTestDomain();

    @Key("desktop.uri")
    @DefaultValue("https://test.avto.ru")
    URI getDesktopURI();

    @Key("autoru.prod.uri")
    @DefaultValue("https://auto.ru")
    URI getAutoruProdURI();

    @Key("autoru.prod.domain")
    @DefaultValue("auto.ru")
    String getAutoruProdDomain();

    @Key("desktop.testing.uri")
    @DefaultValue("https://test.avto.ru")
    URI getTestingURI();

    @Key("testing.domain")
    @DefaultValue("test.avto.ru")
    String getTestingDomain();

    @Key("desktop.yandexGeoCookieName")
    @DefaultValue("yandex_gid")
    String getYandexGeoCookieName();

    @Key("desktop.autoruGeoCookieName")
    @DefaultValue("gids")
    String getAutoruGeoCookieName();

    @Key("proxy.timeout.connect")
    @DefaultValue("5")
    int getProxyTimeoutConnect();

    @Key("desktop.configuration.discount")
    @DefaultValue("false")
    boolean isDiscountDays();

    @Key("desktop.configuration.discount.cookie.name")
    @DefaultValue(DesktopStatic.COOKIE_NAME_DISCOUNT_DAY)
    String getDiscountCookieName();

    @Key("desktop.configuration.discount.cookie.value")
    @DefaultValue("closed")
    String getDiscountCookieValue();

    @Key("desktop.configuration.experiment.flags")
    @DefaultValue("")
    String getExperimentFlags();

    @Key("desktop.configuration.ads.cookie.name")
    @DefaultValue("")
    String getAdsCookieName();

    @Key("desktop.configuration.ads.cookie.value")
    @DefaultValue("")
    String getAdsCookieValue();

    @Key("desktop.configuration.los.cookie.name")
    @DefaultValue("")
    String getLosCookieName();

    @Key("desktop.configuration.los.cookie.value")
    @DefaultValue("")
    String getLosCookieValue();

    @Key("desktop.configuration.gdpr.cookie.name")
    @DefaultValue("")
    String getGdprCookieName();

    @Key("desktop.configuration.gdpr.cookie.value")
    @DefaultValue("")
    String getGdprCookieValue();

    @Key("branch.cookie.name")
    @DefaultValue("_branch")
    String getBranchCookieName();

    @Key("branch.cookie.value")
    @DefaultValue("")
    String getBranchCookieValue();

    @Key("aab_partner_key")
    @DefaultValue("")
    String getAabPartnerKey();

    @Key("mobile.uri")
    @DefaultValue("https://m.test.avto.ru")
    String getMobileURI();

    @Key("desktop.avatars.uri")
    @DefaultValue("https://avatars.mds.yandex.net")
    String getAvatarsURI();

    @Key("bunker.api.url")
    @DefaultValue("http://bunker-api.yandex.net")
    String getBunkerApiUrl();

    @Key("tus.token")
    String getTusToken();

    @Key("tus.consumer")
    @DefaultValue("autoru")
    String getTusConsumer();

    @Key("tus.enviroment")
    @DefaultValue("prod")
    String getTusEnviroment();

    @Key("webdriver.remote.url")
    String getWebdriverRemoteUrl();

}
