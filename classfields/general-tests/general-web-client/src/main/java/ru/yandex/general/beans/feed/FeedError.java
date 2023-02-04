package ru.yandex.general.beans.feed;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class FeedError {

    String title;
    String message;
    String errorType;
    String externalOfferId;
    String detailedDescription;

    public static FeedError feedError() {
        return new FeedError();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
