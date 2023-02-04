package ru.yandex.general.beans.ajaxRequests;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.card.Address;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class User {

    List<Address> addresses;
    String ymlPhone;

    public static User user() {
        return new User();
    }

}
