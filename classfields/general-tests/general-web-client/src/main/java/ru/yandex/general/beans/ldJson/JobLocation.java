package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class JobLocation {

    @SerializedName("@type")
    String type;
    Address address;

    public static JobLocation jobLocation(Address address) {
        return new JobLocation().setType("Place").setAddress(address);
    }

}
