package ru.yandex.general.beans.graphql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CategorySearchRequest {

    Area area;

    public static CategorySearchRequest categorySearchRequest() {
        return new CategorySearchRequest();
    }

}
