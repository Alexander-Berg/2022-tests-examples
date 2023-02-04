package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Filters {

    @SerializedName("catalog_filter")
    List<CatalogFilter> catalogFilter;
    @SerializedName("in_stock")
    String inStock;
    @SerializedName("year_from")
    String yearFrom;
    @SerializedName("year_to")
    String yearTo;
    @SerializedName("price_from")
    String priceFrom;
    @SerializedName("price_to")
    String priceTo;
    @SerializedName("vin_codes")
    List<String> vinCodes;
    @SerializedName("vin_report_statuses")
    List<String> vinReportStatuses;

    public static Filters filters() {
        return new Filters();
    }

}
