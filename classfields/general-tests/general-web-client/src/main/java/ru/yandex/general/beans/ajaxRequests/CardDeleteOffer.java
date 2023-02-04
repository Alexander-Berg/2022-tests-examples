package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CardDeleteOffer {

    String id;

    public static CardDeleteOffer cardDeleteOffer() {
        return new CardDeleteOffer();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
