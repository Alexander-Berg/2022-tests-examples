package ru.yandex.general.beans.graphql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Area {

    Toponyms toponyms;

    public static Area area() {
        return new Area();
    }

}

