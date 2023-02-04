package ru.auto.tests.desktop.mock.beans.dealerOfferDiscounts;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class DiscountOptions {

    @SerializedName("max_discount")
    Integer maxDiscount;
    Integer tradein;
    Integer insurance;
    Integer credit;

    public static DiscountOptions discountOptions() {
        return new DiscountOptions();
    }

}
