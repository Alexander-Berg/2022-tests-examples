package ru.yandex.general.beans.toponyms;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Region {

    String regionId;
    String name;

    public static Region region() {
        return new Region();
    }

}
