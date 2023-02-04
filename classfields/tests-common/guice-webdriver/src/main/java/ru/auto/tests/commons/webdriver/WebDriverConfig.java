package ru.auto.tests.commons.webdriver;

import org.aeonbits.owner.Config;

import java.net.URL;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
@Config.Sources("classpath:testing.properties")
public interface WebDriverConfig extends Config {

    @Key("webdriver.local")
    @DefaultValue("false")
    boolean isLocal();

    @DefaultValue("vertistest")
    @Key("webdriver.remote.username")
    String getRemoteUsername();

    @DefaultValue("ebeb5544e20cc41da95de1a9096068da")
    @Key("webdriver.remote.password")
    String getRemotePassword();

    @Key("webdriver.browser.name")
    @DefaultValue("firefox")
    String getBrowserName();

    @Key("webdriver.browser.version")
    String getBrowserVersion();

    @Key("webdriver.browser.useragent")
    String getUserAgent();

    @Key("webdriver.browser.emulation")
    String getEmulation();

    @Key("webdriver.remote.url")
    URL getRemoteUrl();

    @DefaultValue("true")
    @Key("webdriver.video.enabled")
    boolean videoEnabled();

    @DefaultValue("false")
    @Key("webdriver.video.enabled")
    boolean logsEnabled();

    @DefaultValue("240")
    @Key("webdriver.browser.start.timeout")
    long getBrowserStartTimeout();
}
