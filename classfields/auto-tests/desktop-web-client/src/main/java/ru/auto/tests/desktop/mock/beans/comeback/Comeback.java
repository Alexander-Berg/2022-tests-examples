package ru.auto.tests.desktop.mock.beans.comeback;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Comeback {

    Filter filter;
    String sorting;
    Pagination pagination;

    public static Comeback comeback() {
        return new Comeback();
    }

}
