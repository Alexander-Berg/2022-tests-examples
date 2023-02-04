package ru.auto.tests.vos2;

import org.aeonbits.owner.Config;

/**
 * User: timondl@yandex-team.ru
 * Date: 05.10.16
 */
@Config.Sources("classpath:testing.properties")
public interface VosConfig extends Config {

    @Key("vos.baseUrl")
    @DefaultValue("http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net")
    String getBaseUrl();

    @Key("vos.basePort")
    @DefaultValue("80")
    int getPort();

}
