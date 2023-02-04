package ru.yandex.general.beans.metrics;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class EventAction {

    List<Product> products;

    public static EventAction eventAction() {
        return new EventAction();
    }

}
