package ru.auto.tests.desktop.mock.beans.stub;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Headers {

    @SerializedName("Content-Type")
    String contentType;

    public static Headers headers() {
        return new Headers();
    }
}
