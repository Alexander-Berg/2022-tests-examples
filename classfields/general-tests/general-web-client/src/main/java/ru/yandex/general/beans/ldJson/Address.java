package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Address {

    @SerializedName("@type")
    String type;
    String streetAddress;
    String addressLocality;
    String postalCode;

    public static Address address() {
        return new Address().setType("PostalAddress");
    }

}
