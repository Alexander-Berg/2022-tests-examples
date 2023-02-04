package ru.yandex.general.beans.graphql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Variables {

    String id;
    long userId;
    CategorySearchRequest categorySearchRequest;
    int daysPeriod;
    Boolean hasAuth;

    public static Variables variables() {
        return new Variables();
    }

}
