package ru.yandex.general.beans.metrics;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class EcommerceEvent {

    EventAction detail;
    EventAction purchase;

    public static EcommerceEvent ecommerceEvent(){
        return new EcommerceEvent();
    }

}
