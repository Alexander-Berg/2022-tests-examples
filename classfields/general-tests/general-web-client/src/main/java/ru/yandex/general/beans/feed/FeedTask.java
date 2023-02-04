package ru.yandex.general.beans.feed;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class FeedTask {

    String taskId;
    String status;
    String finishedAt;
    List<FatalError> fatalErrors;
    FeedStatistics feedStatistics;

    public static FeedTask feedTask() {
        return new FeedTask();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
