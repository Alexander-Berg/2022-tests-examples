package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.feed.FeedTask;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.mock.MockTasks.failedTask;
import static ru.yandex.general.mock.MockTasks.inProgressTask;
import static ru.yandex.general.mock.MockTasks.succeedTask;

public class MockTaskInfo {

    private static final String FEED_STATISTICS = "feedStatistics";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject taskInfo;

    private MockTaskInfo(String pathToTemplate) {
        this.taskInfo = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public MockTaskInfo(FeedTask feedTask) {
        this.taskInfo = new Gson().toJsonTree(feedTask).getAsJsonObject();
    }

    public static MockTaskInfo mockTaskInfo(String pathToResource) {
        return new MockTaskInfo(pathToResource);
    }

    public static MockTaskInfo taskInProgress() {
        return new MockTaskInfo(inProgressTask());
    }

    public static MockTaskInfo taskSuccess() {
        return new MockTaskInfo(succeedTask());
    }

    public static MockTaskInfo taskFailed() {
        return new MockTaskInfo(failedTask());
    }

    @Step("Добавляем feedStatistics.totalOfferCount = «{count}»")
    public MockTaskInfo setTotalOfferCount(int count) {
        taskInfo.getAsJsonObject(FEED_STATISTICS).addProperty("totalOfferCount", count);
        return this;
    }

    @Step("Добавляем feedStatistics.activeOfferCount = «{count}»")
    public MockTaskInfo setActiveOfferCount(int count) {
        taskInfo.getAsJsonObject(FEED_STATISTICS).addProperty("activeOfferCount", count);
        return this;
    }

    @Step("Добавляем feedStatistics.errorCount = «{count}»")
    public MockTaskInfo setErrorCount(int count) {
        taskInfo.getAsJsonObject(FEED_STATISTICS).addProperty("errorCount", count);
        return this;
    }

    @Step("Добавляем feedStatistics.criticalErrorCount = «{count}»")
    public MockTaskInfo setCriticalErrorCount(int count) {
        taskInfo.getAsJsonObject(FEED_STATISTICS).addProperty("criticalErrorCount", count);
        return this;
    }

    @Step("Добавляем taskId = «{taskId}»")
    public MockTaskInfo setTaskId(String taskId) {
        taskInfo.addProperty("taskId", taskId);
        return this;
    }

    public String build() {
        return taskInfo.toString();
    }

}
