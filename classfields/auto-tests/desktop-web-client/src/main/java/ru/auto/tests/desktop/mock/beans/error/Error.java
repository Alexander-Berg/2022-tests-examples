package ru.auto.tests.desktop.mock.beans.error;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Error {

    String error;
    String status;
    @SerializedName("detailed_error")
    String detailedError;

    public static Error error() {
        return new Error();
    }

}
