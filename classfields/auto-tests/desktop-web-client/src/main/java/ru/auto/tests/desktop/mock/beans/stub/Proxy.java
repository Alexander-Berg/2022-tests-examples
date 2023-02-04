package ru.auto.tests.desktop.mock.beans.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Proxy {

    String to;
    String mode;

    public static Proxy proxy() {
        return new Proxy();
    }
}
