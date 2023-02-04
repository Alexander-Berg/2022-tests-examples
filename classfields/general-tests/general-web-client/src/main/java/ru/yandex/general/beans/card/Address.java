package ru.yandex.general.beans.card;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.metrics.Ym;

@Setter
@Getter
@Accessors(chain = true)
public class Address {

    AddressText address;
    GeoPoint geoPoint;
    MetroStation metroStation;
    District district;
    Region region;
    String name;

    public static Address address() {
        return new Address();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
