package ru.auto.tests.salesman.user.config;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface SalesmanUserApiConfig extends Config {

    @Key("salesman-user.baseUrl")
    @DefaultValue("http://salesman-user-api-http-api.vrts-slb.test.vertis.yandex.net/api/1.x/")
    String getSalesmanUserUrl();
}
