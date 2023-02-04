package ru.yandex.realty.config;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:testing.properties")
public interface RealtyTagConfig extends RealtyWebConfig {

    @Key("tags.region")
    String getRegion();

    @Key("tags.metro")
    String getMetro();

    @Key("tags.sublocality")
    String getSubLocality();

    @Key("tags.direction")
    String getDirection();

    @Key("tags.rgid")
    String getRgid();
}