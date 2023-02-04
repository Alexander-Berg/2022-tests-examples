package ru.yandex.general.beans.events;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Event {

    EventInfo eventInfo;
    String eventTime;
    Context context;
    String queryId;
    TrafficSource trafficSource;
    String portalRegionId;
    String regionId;

    public static Event event() {
        return new Event();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
