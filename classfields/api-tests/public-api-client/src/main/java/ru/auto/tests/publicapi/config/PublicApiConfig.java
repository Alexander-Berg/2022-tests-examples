package ru.auto.tests.publicapi.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface PublicApiConfig extends Accessible {

    @Key("public.api.testing.uri")
    @DefaultValue("http://autoru-api-server.vrts-slb.test.vertis.yandex.net:80")
    URI getPublicApiTestingURI();

    @Key("public.api.release.uri")
    @DefaultValue("http://autoru-api-production-server.vrts-slb.test.vertis.yandex.net:80")
    URI getPublicApiProdURI();

    @Key("public.api.version")
    @DefaultValue("1.0")
    String getPublicApiVersion();

    @Key("vos.api.uri")
    @DefaultValue("http://vos2-autoru-api-server.vrts-slb.test.vertis.yandex.net:80/api/v1")
    URI getVosApiURI();

    @Key("chat.api.uri")
    @DefaultValue("http://chat-api-auto-server.vrts-slb.test.vertis.yandex.net:80/api/1.x")
    URI getChatApiURI();

    @Key("pushnoy.api.uri")
    @DefaultValue("http://pushnoy-api-http-api.vrts-slb.test.vertis.yandex.net:80/api/v1/")
    URI getPushnoyApiURI();

    @Key("shark.api.uri")
    @DefaultValue("http://shark-api-main.vrts-slb.test.vertis.yandex.net:80/api/1.x/")
    URI getSharkApiURI();

    @Key("json.ignoring.options.enabled")
    @DefaultValue("true")
    boolean isJsonIgnoringOptionsEnabled();

    @Key("rest.assured.logger.enabled")
    @DefaultValue("true")
    boolean isRestAssuredLoggerEnabled();

    @Key("jaeger.uri")
    @DefaultValue("https://jaeger.test.vertis.yandex.net")
    URI getJaegerUri();

    @Key("safe-deal.api.uri")
    @DefaultValue("http://safe-deal-api-main.vrts-slb.test.vertis.yandex.net/api/1.x")
    URI getSafeDealApiURI();

    @Key("tamper_salt")
    @DefaultValue("")
    String getTamperSalt();
}
