package ru.yandex.realty.beans.developer.office;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Coordinates {

    private double latitude;
    private double longitude;
    private boolean defined;

}
