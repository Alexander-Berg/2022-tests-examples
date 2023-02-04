package ru.yandex.general.beans.ajaxRequests.updateDraft;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.card.AddressText;
import ru.yandex.general.beans.card.District;
import ru.yandex.general.beans.ajaxRequests.updateDraft.GeoPoint;
import ru.yandex.general.beans.card.MetroStation;
import ru.yandex.general.beans.card.Region;
import ru.yandex.general.utils.NullStringTypeAdapter;

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
