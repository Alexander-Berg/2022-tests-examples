package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.page.FeedPage;

import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.yandex.general.element.Input.FILE;
import static ru.yandex.general.page.FeedPage.PROCESSING;

public class FeedSteps extends BasePageSteps {

    private static final String DEFAULT_FEED = "src/test/resources/feed/feedExample.xml";
    public static final String YML_FEED = "src/test/resources/feed/ymlFeed.yml";

    @Inject
    private GeneralWebConfig config;

    @Step("Добавляем фид")
    public void addFeed(String pathToResource) {
        setFileDetector();
        onFeedPage().inputWithType(FILE).sendKeys(new File(pathToResource).getAbsolutePath());
    }

    @Step("Добавляем фид в модалке")
    public void addFeedInModal(String pathToResource) {
        setFileDetector();
        onFeedPage().modal().inputWithType(FILE).sendKeys(new File(pathToResource).getAbsolutePath());
    }

    @Step("Ждем обработку фида")
    public void waitUntilFeedProcessed() {
        await().atMost(90, SECONDS).pollInterval(5, SECONDS)
                .until(() -> {
                    refresh();
                    return !onFeedPage().feedStatus().getText().equals(PROCESSING);
                });
    }

    public void addFeedExample() {
        addFeed(DEFAULT_FEED);
    }

    public void addFeedExampleInModal() {
        addFeedInModal(DEFAULT_FEED);
    }

    private static String getDefaultFeedPath() {
        return new File("src/test/resources/feed/feedExample.xml").getAbsolutePath();
    }

    private void setFileDetector() {
        if (!config.isLocalDebug()) {
            ((RemoteWebDriver) getDriver()).setFileDetector(new LocalFileDetector());
        }
    }
}
