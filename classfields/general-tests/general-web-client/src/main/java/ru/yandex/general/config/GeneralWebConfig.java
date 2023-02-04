package ru.yandex.general.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;
import java.net.URL;

@Config.Sources("classpath:testing.properties")
public interface GeneralWebConfig extends Accessible {

    @Key("general.testing.uri")
    @DefaultValue("https://o.test.vertis.yandex.ru/")
    URI getTestingURI();

    @Key("general.production.uri")
    URI getProductionURI();

    @Key("general.realProduction.uri")
    @DefaultValue("https://o.yandex.ru/")
    URI getRealProductionURI();

    @Key("mockritsa.url")
    @DefaultValue("http://vertis-mockritsa-api.vrts-slb.test.vertis.yandex.net")
    String getMockritsaURL();

    @Key("baseDomain")
    @DefaultValue(".o.test.vertis.yandex.ru")
    String getBaseDomain();

    @Key("passport.testing.url")
    @DefaultValue("https://passport-test.yandex.ru/")
    URL getPassportTestURL();

    @Key("passport.prod.url")
    @DefaultValue("https://passport.yandex.ru/")
    URL getPassportProdURL();

    @Key("compare.diff.size")
    @DefaultValue("10")
    int getDiffSize();

    @Key("is.local.debug")
    @DefaultValue("false")
    boolean isLocalDebug();

    @Key("tus.token")
    String getTusToken();

    @Key("tus.consumer")
    @DefaultValue("general")
    String getTusConsumer();

    @Key("tus.enviroment")
    @DefaultValue("prod")
    String getTusEnviroment();

}
