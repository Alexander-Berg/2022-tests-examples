package ru.yandex.general.beans.graphql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Toponyms {

    String region;

    public static Toponyms toponyms() {
        return new Toponyms();
    }

}

