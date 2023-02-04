package ru.auto.tests.desktop.mock.beans.carfaxReport;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class AutoruOffers {

    Header header;
    List<Offer> offers;
    String status;
    @SerializedName("record_count")
    Integer recordCount;

    public static AutoruOffers autoruOffers() {
        return new AutoruOffers();
    }

}
