package ru.auto.tests.commons.video;

import io.qameta.allure.Attachment;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.extern.log4j.Log4j;
import ru.auto.tests.commons.webdriver.WebDriverConfig;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 01.08.18
 */
@Deprecated()
/** @deprecated. @see ru.auto.tests.commons.webdriver.DefaultWebDriverManager#attachTestVideo()*/
@Log4j
public class TestVideoProvider {
    @Inject
    private WebDriverConfig config;

    @Attachment(value = "Видео выполнения теста", type = "video/mp4")
    public byte[] testVideo(String sessionId) {
        byte[] video = new byte[] {};
        try {
            log.info("Trying to download video");
            try {
                await().ignoreExceptions().atMost(10, SECONDS).pollInterval(1, SECONDS)
                    .until(() -> given()
                        .auth().basic(config.getRemoteUsername(), config.getRemotePassword())
                        .get("http://sg.yandex-team.ru:4444/video/" + sessionId)
                        .getStatusCode() == 200);
            } catch (org.awaitility.core.ConditionTimeoutException ignore) {
            }

            video = given()
                .filters(new RequestLoggingFilter(), new ResponseLoggingFilter(LogDetail.STATUS))
                .auth().basic(config.getRemoteUsername(), config.getRemotePassword())
                .expect()
                .statusCode(200)
                .when()
                .get("http://sg.yandex-team.ru:4444/video/" + sessionId)
                .asByteArray();

        } catch (Throwable ignore) {
            log.error("Failed to download test video");
        }

        return video;
    }
}
