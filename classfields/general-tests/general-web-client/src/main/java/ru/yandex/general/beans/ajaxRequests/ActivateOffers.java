package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class ActivateOffers {

    List<String> offerIds;
    int page;
    String preset;

    public static ActivateOffers activateOffers() {
        return new ActivateOffers().setPage(1);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
