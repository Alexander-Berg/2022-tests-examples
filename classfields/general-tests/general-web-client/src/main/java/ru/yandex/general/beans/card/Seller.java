package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Seller {

    String name;
    String sellerType;
    String avatar;

}
