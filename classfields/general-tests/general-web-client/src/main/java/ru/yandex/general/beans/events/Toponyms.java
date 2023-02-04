package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Toponyms {

    String region;
    List<String> metro;
    List<String> districts;

    public static Toponyms toponyms() {
        return new Toponyms();
    }

}
