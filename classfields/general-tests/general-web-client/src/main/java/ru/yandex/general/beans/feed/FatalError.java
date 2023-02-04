package ru.yandex.general.beans.feed;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class FatalError {

    String message;
    String description;
    String requiredAction;

    public static FatalError fatalError() {
        return new FatalError();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
