package ru.yandex.arenda.config;

import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface ArendaWebConfig extends Config {

    @Key("arenda.testing.uri")
    String getTestingURI();

    @Key("arenda.production.uri")
    URI getProductionURI();

    @Key("compare.diff.size")
    @DefaultValue("10")
    int getDiffSize();

    @Key("passport.testing.url")
    URI getPassportTestURL();

    @Key("mockritsa.url")
    @DefaultValue("http://mockritsa-realty-api.vrts-slb.test.vertis.yandex.net")
    String getMockritsaURL();

    @Key("is.local.debug")
    @DefaultValue("true")
    boolean isLocalDebug();
}