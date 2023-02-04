package ru.yandex.general.beans.graphql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Request {

    String query;
    Variables variables;

    public static Request request() {
        return new Request();
    }

}
