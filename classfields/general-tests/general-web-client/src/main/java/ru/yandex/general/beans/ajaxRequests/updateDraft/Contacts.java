package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Contacts {

    String phone;
    String email;
    String preferredWayToContact;

    public static Contacts contacts() {
        return new Contacts();
    }

}
