package ru.auto.tests.passport.config;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface PassportConfig extends Config {

    @Key("is.local.debug")
    @DefaultValue("true")
    boolean isLocalDebug();

    @Key("passport.env")
    @DefaultValue("test")
    String env();

    @Key("passport.proxy.host")
    @DefaultValue("kurau.dev.vertis.yandex.net")
    String proxyHost();

    @Key("passport.proxy.port")
    @DefaultValue("3128")
    String proxyPort();

    @Key("passport.test.internal.url")
    @DefaultValue("http://passport-test-internal.yandex.ru/1/")
    String getYandexPassportInternalTestUrl();

    @Key("passport.consumer")
    @DefaultValue("vertis")
    String consumer();

    @Key("pass.test")
    @DefaultValue("http://pass-test.yandex.ru/")
    String getPassTestUrl();

    @Key("api.captcha")
    @DefaultValue("http://api.captcha.yandex.net/")
    String getApiCapthaUrl();
}
