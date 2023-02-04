package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class SearchArea {

    Toponyms toponyms;
    Coordinates coordinates;

    public static SearchArea searchArea() {
        return new SearchArea();
    }

}
