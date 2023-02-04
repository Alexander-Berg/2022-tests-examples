package ru.auto.tests.canonical;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface CanonicalConfig extends Config {

    @Key("branch.cookie.name")
    @DefaultValue("_branch")
    String getBranchCookieName();

    @Key("branch.cookie.value")
    @DefaultValue("autotest")
    String getBranchCookieValue();
}