package ru.auto.tests.desktop.mock.beans.billing;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class DetailedProductInfo {

    @SerializedName("prolongation_allowed")
    boolean prolongationAllowed;
    String duration;
    @SerializedName("base_price")
    Integer basePrice;
    @SerializedName("auto_prolong_price")
    Integer autoProlongPrice;
    @SerializedName("effective_price")
    Integer effectivePrice;
    String service;
    String name;
    Integer days;
    @SerializedName("prolongation_forced")
    Boolean prolongationForced;
    @SerializedName("prolongation_forced_not_togglable")
    Boolean prolongationForcedNotTogglable;

    public static DetailedProductInfo detailedProductInfo() {
        return new DetailedProductInfo();
    }

}
