package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Arrays.asList;

@Setter
@Getter
@Accessors(chain = true)
public class MarketingCampaignsSettings {

    List<String> marketingCampaignsSettings;

    public static MarketingCampaignsSettings marketingCampaignsSettings(String... settings) {
        return new MarketingCampaignsSettings().setMarketingCampaignsSettings(asList(settings));
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
