package ru.yandex.general.beans.toponyms;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Position {

    String latitude;
    String longitude;

    public static Position position() {
        return new Position();
    }

}
