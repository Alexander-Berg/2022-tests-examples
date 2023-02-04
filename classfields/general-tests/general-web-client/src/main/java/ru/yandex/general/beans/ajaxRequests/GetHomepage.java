package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class GetHomepage {

    int limit;
    int page;
    String pageToken;
    String regionId;

    public static GetHomepage getHomepage() {
        return new GetHomepage();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
