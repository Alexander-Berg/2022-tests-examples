package ru.yandex.general.beans.ajaxRequests.offerListingService;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Toponyms {

    List<String> metro;
    String region;
    List<String> districts;

    public static Toponyms toponyms() {
        return new Toponyms();
    }

}
