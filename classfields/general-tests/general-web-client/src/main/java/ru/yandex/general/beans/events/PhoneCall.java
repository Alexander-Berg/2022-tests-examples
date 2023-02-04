package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PhoneCall {

    Offer offer;

    public static PhoneCall phoneCall() {
        return new PhoneCall();
    }

}
