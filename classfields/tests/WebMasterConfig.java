package ru.yandex.webmaster.tests;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface WebMasterConfig extends Config {

    @Key("is.local.debug")
    @DefaultValue("true")
    boolean isLocalDebug();

    @Key("webmaster.prod.login")
    String wmLogin();

    @Key("webmaster.prod.password")
    String wmPassword();

    @Key("webmaster.feeds.url")
    String feedsUrl();

    @Key("webmaster.url")
    String wmUrl();
}
