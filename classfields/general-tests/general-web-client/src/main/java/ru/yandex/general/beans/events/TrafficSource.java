package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TrafficSource {

    public static TrafficSource trafficSource() {
        return new TrafficSource();
    }

}
