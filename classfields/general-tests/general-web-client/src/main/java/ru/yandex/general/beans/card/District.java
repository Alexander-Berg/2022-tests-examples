package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class District {

    String id;
    String name;
    Object isEnriched;

    public static District district() {
        return new District();
    }

}
