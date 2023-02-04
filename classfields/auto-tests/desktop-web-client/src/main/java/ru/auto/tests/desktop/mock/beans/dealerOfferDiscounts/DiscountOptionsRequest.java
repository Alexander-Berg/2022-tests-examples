package ru.auto.tests.desktop.mock.beans.dealerOfferDiscounts;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class DiscountOptionsRequest {

    @SerializedName("discount_options")
    DiscountOptions discountOptions;

    public static DiscountOptionsRequest discountOptionsRequest() {
        return new DiscountOptionsRequest();
    }

}
