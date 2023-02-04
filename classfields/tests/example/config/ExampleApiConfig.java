package ru.auto.tests.example.config;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

import java.net.URI;

@Config.Sources("classpath:testing.properties")
public interface ExampleApiConfig extends Accessible {

    @Key("example.api.testing.uri")
    @DefaultValue("http://petstore.swagger.io:80")
    URI getExampleApiTestingURI();

    @Key("example.api.release.uri")
    @DefaultValue("http://petstore.swagger.io:80")
    URI getExampleApiProdURI();

    @Key("example.api.version")
    @DefaultValue("v2")
    String getExampleApiVersion();

    @Key("rest.assured.logger.enabled")
    @DefaultValue("true")
    boolean isRestAssuredLoggerEnabled();
}
