package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Offer {

    String offerId;
    String offerVersion;
    String categoryId;
    Price price;
    String firstPhotoUrl;
    int photoCount;
    String colorHex;
    String snippetRatio;

    public static Offer offer() {
        return new Offer();
    }

}
