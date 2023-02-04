package ru.auto.tests.desktop.consts;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author kurau (Yuri Kalinin)
 */
@Getter
@Setter
@Accessors(chain = true)
public class CarEngineType {

    private String name;
    private String shortName;
    private String adjectivePlural;
    private String adjective;

}
