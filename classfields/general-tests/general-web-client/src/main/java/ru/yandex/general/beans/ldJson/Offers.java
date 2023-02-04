package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Offers {

    @SerializedName("@type")
    String type;
    String url;
    String priceCurrency;
    Object price;
    String itemCondition;
    String availability;

    public static Offers offers() {
        return new Offers().setType("Offer").setPriceCurrency("RUB").setAvailability("https://schema.org/InStock");
    }

}
