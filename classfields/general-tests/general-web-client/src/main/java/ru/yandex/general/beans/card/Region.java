package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Region {

    String id;
    String name;

    public static Region region() {
        return new Region();
    }

}
