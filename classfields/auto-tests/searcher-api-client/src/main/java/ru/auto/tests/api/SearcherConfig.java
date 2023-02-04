package ru.auto.tests.api;

import org.aeonbits.owner.Config;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */

@Config.Sources("classpath:testing.properties")
public interface SearcherConfig extends Config {

    @Key("searcher.baseUrl")
    @DefaultValue("http://auto2-searcher-api.vrts-slb.test.vertis.yandex.net:80")
    String getBaseUrl();
}
