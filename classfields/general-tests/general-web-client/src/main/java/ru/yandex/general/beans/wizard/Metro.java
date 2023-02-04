package ru.yandex.general.beans.wizard;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Metro {

    String distance;
    String color;
    String name;

    public static Metro metro() {
        return new Metro();
    }

}
