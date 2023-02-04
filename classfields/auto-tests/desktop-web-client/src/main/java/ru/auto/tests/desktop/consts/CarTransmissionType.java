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
public class CarTransmissionType {

    private String name;
    private String shortName;
    private String fullName;

    public static CarTransmissionType transmission() {
        return new CarTransmissionType();
    }

}
