package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CardActivateOffer {

    String id;

    public static CardActivateOffer cardActivateOffer() {
        return new CardActivateOffer();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
