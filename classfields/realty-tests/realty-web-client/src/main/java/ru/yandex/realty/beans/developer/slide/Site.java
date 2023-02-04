package ru.yandex.realty.beans.developer.slide;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Site {

    String id;
    String name;
    String rgid;

}
