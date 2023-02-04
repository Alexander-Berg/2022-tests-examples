package ru.auto.tests.passport.config;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface PassportApiConfig extends Config {

    @Key("passport.baseUrl")
    @DefaultValue("http://passport-api-server.vrts-slb.test.vertis.yandex.net/api/2.x/")
    String getPassportUrl();
}
