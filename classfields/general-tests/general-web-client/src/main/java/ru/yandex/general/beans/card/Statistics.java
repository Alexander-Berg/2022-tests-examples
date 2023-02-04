package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Statistics {

    int viewsCount;
    int favoritesCount;
    int contactsCount;

    public static Statistics statistics() {
        return new Statistics();
    }

}
