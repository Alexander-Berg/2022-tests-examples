package ru.auto.tests.desktop.mock.beans.stub;

import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Is {

    Headers headers;
    JsonElement body;
    int statusCode;

    public static Is is() {
        return new Is();
    }
}
