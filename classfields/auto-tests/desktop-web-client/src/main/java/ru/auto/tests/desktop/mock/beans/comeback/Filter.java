package ru.auto.tests.desktop.mock.beans.comeback;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Filter {

    @SerializedName("creation_date_to")
    Object creationDateTo;
    @SerializedName("creation_date_from")
    Object creationDateFrom;
    @SerializedName("salon_geo_id")
    List<Object> salonGeoId;
    List<Integer> rid;
    @SerializedName("geo_radius")
    Object geoRadius;
    @SerializedName("page_size")
    Integer pageSize;
    @SerializedName("only_last_seller")
    Boolean onlyLastSeller;

    public Filter setSalonGeoIdNull() {
        List<Object> salon = new ArrayList<>();
        salon.add(null);
        salonGeoId = salon;
        return this;
    }

    public static Filter filter() {
        return new Filter();
    }

}
