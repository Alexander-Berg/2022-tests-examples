package ru.yandex.general.beans.ajaxRequests;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class FeedErrors {

    String taskId;
    int page;
    String filter;
    int limit;

    public static FeedErrors feedErrors() {
        return new FeedErrors();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
