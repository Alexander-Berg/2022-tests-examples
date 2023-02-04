package ru.yandex.realty.config;

import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface RealtyApiConfig extends Config {

    @Key("realty.api.vos2.uri")
    @DefaultValue("http://realty-vos-api-http.vrts-slb.test.vertis.yandex.net:80/api/realty")
    URI getVos2BaseUri();

    @Key("realty.api.vos2.prod.uri")
    @DefaultValue("http://vos2-realty-api-test-int.slb.vertis.yandex.net/api/realty")
    URI getVos2ProdUri();

    @Key("realty.api.searcher.uri")
    @DefaultValue("http://realty-searcher-api.vrts-slb.test.vertis.yandex.net")
    URI getSearcherUri();

    @Key("realty.api.back.uri")
    @DefaultValue("http://subs2-api-http.vrts-slb.test.vertis.yandex.net/api/2.x")
    URI getRealtyBackUri();

    @Key("realty.api.promo.uri")
    @DefaultValue("http://promocoder-api-http-api.vrts-slb.test.vertis.yandex.net:80/api/1.x/service/realty")
    URI getPromoUri();

    @Key("realty.api.bnb.searcher.uri")
    @DefaultValue("http://realty-bnb-searcher-01-sas.test.vertis.yandex.net:36259")
    URI getBnbSearcherUri();

    @Key("is.local.debug")
    @DefaultValue("true")
    boolean isLocalDebug();

    @Key("realty.env")
    @DefaultValue("test")
    String env();
}
