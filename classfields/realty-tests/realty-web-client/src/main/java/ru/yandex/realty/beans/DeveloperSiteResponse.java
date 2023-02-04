package ru.yandex.realty.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class DeveloperSiteResponse {

    private String id;
    private String name;
    private List<String> phones;

    public static DeveloperSiteResponse developer() {
        return new DeveloperSiteResponse();
    }

}
