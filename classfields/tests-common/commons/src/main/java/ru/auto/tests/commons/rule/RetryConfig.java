package ru.auto.tests.commons.rule;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface RetryConfig extends Config {

    @Key("retry.attempts")
    @DefaultValue("0")
    int getAttempts();
}
