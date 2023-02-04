package ru.yandex.realty.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;
import java.net.URL;

/**
 * eroshenkoam
 * 30.01.17
 */
@Config.Sources("classpath:testing.properties")
public interface RealtyWebConfig extends Accessible {

    @Key("realty.production.uri")
    URI getProductionURI();

    @Key("realty.testing.uri")
    URI getTestingURI();

    @Key("realty.experiment.flags")
    @DefaultValue("off:__ALL__,REALTY-12358_stat")
    String getExperimentFlags();

    @Key("realty.prestable")
    @DefaultValue("false")
    boolean isPrestable();

    @Key("realty.docker")
    @DefaultValue("false")
    boolean isDocker();

    @Key("passport.testing.url")
    URL getPassportTestURL();

    @Key("compare.diff.size")
    @DefaultValue("10")
    int getDiffSize();

    @DefaultValue("4444444444444448")
    @Key("realty.testing.card.number")
    String cardNumber();

    @Key("extension.adblock.enable")
    @DefaultValue("false")
    boolean getExtensionAdBlockEnable();

    @Key("mockritsa.url")
    @DefaultValue("http://mockritsa-realty-api.vrts-slb.test.vertis.yandex.net")
    String getMockritsaURL();

    @Key("webdriver.remote.url")
    @DefaultValue("http://selenoid-10-sas.test.vertis.yandex.net:4444/wd/hub")
    URL getSelenoidUrl();

    @Key("set.cookie.rule")
    @DefaultValue("")
    String getCookies();

    @Key("mock.create")
    @DefaultValue("false")
    boolean mockCreate();

    @Key("mock.record")
    @DefaultValue("false")
    boolean mockRecord();

    @Key("mock.attach.imposter")
    @DefaultValue("false")
    boolean mockAttachImposter();

    @Key("baseDomain")
    @DefaultValue("yandex.ru")
    String getBaseDomain();

    @Key("yandex-team.yql.authorization.token")
    @DefaultValue("")
    String getYqlToken();

    @Key("realty.url.timeout")
    @DefaultValue("10")
    int getUrlTimeout();

}