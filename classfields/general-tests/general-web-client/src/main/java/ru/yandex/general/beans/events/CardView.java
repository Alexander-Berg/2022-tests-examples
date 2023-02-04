package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CardView {

    Offer offer;

    public static CardView cardView() {
        return new CardView();
    }

}
