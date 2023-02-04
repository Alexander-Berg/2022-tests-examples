package ru.yandex.general.beans.toponyms;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Line {

    String lineId;
    String name;
    String color;

    public static Line line() {
        return new Line();
    }

}
