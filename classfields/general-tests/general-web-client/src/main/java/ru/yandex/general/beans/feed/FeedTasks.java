package ru.yandex.general.beans.feed;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class FeedTasks {

    int page;

    public static FeedTasks feedTasks() {
        return new FeedTasks();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
