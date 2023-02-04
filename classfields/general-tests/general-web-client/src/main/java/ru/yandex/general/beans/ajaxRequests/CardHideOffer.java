package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CardHideOffer {

    String id;
    String recallReasons;

    public static CardHideOffer cardHideOffer() {
        return new CardHideOffer();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
