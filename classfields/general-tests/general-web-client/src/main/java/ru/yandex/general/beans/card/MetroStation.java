package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class MetroStation {

    String id;
    String name;
    List<String> lineIds;
    List<String> colors;
    Object isEnriched;

    public static MetroStation metroStation() {
        return new MetroStation();
    }

}
