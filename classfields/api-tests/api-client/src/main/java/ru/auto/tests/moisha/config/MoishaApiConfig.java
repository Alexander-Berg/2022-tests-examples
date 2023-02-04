package ru.auto.tests.moisha.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface MoishaApiConfig extends Accessible {

    @Key("example.api.testing.uri")
    @DefaultValue("http://auto2-moisha-api-api.vrts-slb.test.vertis.yandex.net")
    URI getMoishaApiTestingURI();

    @Key("example.api.release.uri")
    @DefaultValue("http://auto2-moisha-api-production-api.vrts-slb.test.vertis.yandex.net")
    URI getMoishaApiProdURI();

    @Key("example.api.version")
    @DefaultValue("api/1.x")
    String getMoishaApiVersion();

    @Key("rest.assured.logger.enabled")
    @DefaultValue("true")
    boolean isRestAssuredLoggerEnabled();
}
