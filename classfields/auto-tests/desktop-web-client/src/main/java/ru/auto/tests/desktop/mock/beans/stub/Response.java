package ru.auto.tests.desktop.mock.beans.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Response {

    Is is;
    Proxy proxy;

    public static Response response() {
        return new Response();
    }

}
