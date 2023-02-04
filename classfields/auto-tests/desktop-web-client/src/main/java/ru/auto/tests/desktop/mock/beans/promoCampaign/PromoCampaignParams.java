package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PromoCampaignParams {

    @SerializedName("promo_campaign_params")
    Campaign promoCampaignParams;

    public static PromoCampaignParams promoCampaignParams() {
        return new PromoCampaignParams();
    }

}
