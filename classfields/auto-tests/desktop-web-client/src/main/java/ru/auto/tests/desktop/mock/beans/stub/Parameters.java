package ru.auto.tests.desktop.mock.beans.stub;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Parameters {

    String path;
    String method;
    JsonObject query;
    JsonObject body;
    JsonObject headers;

    public static Parameters parameters() {
        return new Parameters();
    }

}
