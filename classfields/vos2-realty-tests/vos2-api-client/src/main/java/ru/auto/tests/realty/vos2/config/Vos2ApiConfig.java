package ru.auto.tests.realty.vos2.config;

import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface Vos2ApiConfig extends Config {

    @Key("vos2.api.testing.uri")
    @DefaultValue("http://realty-vos-api-http.vrts-slb.test.vertis.yandex.net")
    URI getVos2ApiTestingURI();

    @Key("vos2.api.release.uri")
    @DefaultValue("http://realty-vos-api-production-http.vrts-slb.test.vertis.yandex.net")
    URI getVos2ApiProdURI();
}
