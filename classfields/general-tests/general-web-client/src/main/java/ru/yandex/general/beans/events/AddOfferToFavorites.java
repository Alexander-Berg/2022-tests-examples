package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class AddOfferToFavorites {

    Offer offer;

    public static AddOfferToFavorites addOfferToFavorites() {
        return new AddOfferToFavorites();
    }

}
