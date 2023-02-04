package ru.auto.tests.cabinet.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface CabinetApiConfig extends Accessible {

    @Key("cabinet.api.testing.uri")
    @DefaultValue("http://autoru-cabinet-api-01-sas.test.vertis.yandex.net:2030/api")
    URI getCabinetApiTestingURI();

    @Key("cabinet.api.release.uri")
    @DefaultValue("http://autoru-cabinet-api-02-sas.test.vertis.yandex.net:2030/api")
    URI getCabinetApiProdURI();

    @Key("cabinet-php.api.uri")
    @DefaultValue("http://lb-int-nginx-01-sas.test.vertis.yandex.net:80/desktop/v1.0.0/client/post/")
    URI getPhpCreateDealerURI();

    @Key("cabinet.api.version")
    @DefaultValue("1.x")
    String getCabinetApiVersion();

    @Key("rest.assured.logger.enabled")
    @DefaultValue("true")
    boolean isRestAssuredLoggerEnabled();
}
