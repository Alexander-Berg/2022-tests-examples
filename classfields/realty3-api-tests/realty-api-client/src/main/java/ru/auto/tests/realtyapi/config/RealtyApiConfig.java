package ru.auto.tests.realtyapi.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources({"classpath:testing.development.properties", "classpath:testing.properties"})
public interface RealtyApiConfig extends Accessible {

    @Key("realty3.api.testing.uri")
    @DefaultValue("http://realty-gateway-api.vrts-slb.test.vertis.yandex.net")
    URI getRealtyApiTestingURI();

    @Key("realty3.api.release.uri")
    @DefaultValue("http://realty-gateway-production-api.vrts-slb.test.vertis.yandex.net")
    URI getRealtyApiProdURI();

    @Key("oauth.token.uri")
    @DefaultValue("http://oauth-test-internal.yandex.ru")
    URI getOAuthTokenURI();

    @Key("realty3.api.v1.path")
    @DefaultValue("1.0")
    String getRealtyApiV1Path();

    @Key("realty3.api.v2.path")
    @DefaultValue("2.0")
    String getRealtyApiV2Path();

    @Key("jaeger.uri")
    @DefaultValue("https://jaeger.test.vertis.yandex.net")
    URI getJaegerUri();

    @Key("rest.assured.logger.enabled")
    @DefaultValue("true")
    boolean isRestAssuredLoggerEnabled();
}
