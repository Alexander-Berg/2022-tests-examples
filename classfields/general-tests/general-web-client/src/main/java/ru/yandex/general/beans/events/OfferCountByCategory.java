package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class OfferCountByCategory {

    String categoryId;
    int offerCount;

    public static OfferCountByCategory offerCountByCategory() {
        return new OfferCountByCategory();
    }

}
