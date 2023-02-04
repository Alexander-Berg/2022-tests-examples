package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.beans.feed.FeedTask;

import java.util.Calendar;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.beans.feed.FatalError.fatalError;
import static ru.yandex.general.beans.feed.FeedStatistics.feedStatistics;
import static ru.yandex.general.beans.feed.FeedTask.feedTask;
import static ru.yandex.general.utils.Utils.getCalendar;
import static ru.yandex.general.utils.Utils.getISOFormatedDate;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;

public class MockTasks {

    private static final String FEED_TASKS_TEMPLATE = "mock/feed/feedTasksTemplate.json";
    private static final String FEED_TASKS_EXAMPLE = "mock/feed/feedTasksExample.json";

    private static final String FAILED = "Failed";
    private static final String IN_PROGRESS = "InProgress";
    private static final String SUCCEED = "Succeed";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject tasks;

    private MockTasks(String pathToTemplate) {
        this.tasks = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockTasks mockFeedTasks(String pathToResource) {
        return new MockTasks(pathToResource);
    }

    public static MockTasks feedTasksTemplate() {
        return new MockTasks(FEED_TASKS_TEMPLATE);
    }

    public static MockTasks feedTasksExample() {
        return new MockTasks(FEED_TASKS_EXAMPLE);
    }

    @Step("Добавляем totalPageCount = «{totalPageCount}»")
    public MockTasks setTotalPageCount(int totalPageCount) {
        tasks.addProperty("totalPageCount", totalPageCount);
        return this;
    }

    @Step("Добавляем currentPage = «{currentPage}»")
    public MockTasks setCurrentPage(int currentPage) {
        tasks.addProperty("currentPage", currentPage);
        return this;
    }

    @Step("Добавляем историю загрузки фидов")
    public MockTasks setTasks(FeedTask... tasks) {
        asList(tasks).stream().forEach(error ->
                this.tasks.getAsJsonArray("tasks").add(new Gson().toJsonTree(error).getAsJsonObject()));
        return this;
    }

    public static FeedTask succeedTask() {
        return feedTask().setTaskId(String.valueOf(getRandomIntInRange(1, 50)))
                .setStatus(SUCCEED)
                .setFinishedAt(getRandomDateTimeInPast())
                .setFatalErrors(null)
                .setFeedStatistics(feedStatistics()
                        .setTotalOfferCount(getRandomIntInRange(100, 200))
                        .setActiveOfferCount(getRandomIntInRange(50, 100))
                        .setErrorCount(getRandomIntInRange(50, 100))
                        .setCriticalErrorCount(getRandomIntInRange(0, 50)));
    }

    public static FeedTask inProgressTask() {
        return feedTask().setTaskId(String.valueOf(getRandomIntInRange(1, 50)))
                .setStatus(IN_PROGRESS)
                .setFinishedAt(null)
                .setFeedStatistics(null)
                .setFatalErrors(null);
    }

    public static FeedTask failedTask() {
        return feedTask().setTaskId(String.valueOf(getRandomIntInRange(1, 50)))
                .setStatus(FAILED)
                .setFinishedAt(getRandomDateTimeInPast())
                .setFeedStatistics(null)
                .setFatalErrors(asList(
                        fatalError().setMessage("Ошибки в структуре файла – строка 13, столбец – 2.")
                                .setDescription("Сверьтесь с <a href=\"https://yandex.ru/support/o-desktop/" +
                                        "price-list-requirements.html\" target=\"_blank\">нашими требованиями</a>" +
                                        " или правилами")));
    }

    public static String getRandomDateTimeInPast() {
        Calendar calendar = getCalendar();
        calendar.add(Calendar.DATE, -getRandomIntInRange(1, 30));
        return getISOFormatedDate(calendar.getTime());
    }

    public String build() {
        return tasks.toString();
    }

}
