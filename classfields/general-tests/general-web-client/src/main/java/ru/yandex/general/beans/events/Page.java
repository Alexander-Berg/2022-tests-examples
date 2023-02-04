package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Page {

    int limit;
    int page;

    public static Page page() {
        return new Page();
    }

}
