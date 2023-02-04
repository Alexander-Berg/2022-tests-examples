package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.realty.config.RealtyWebConfig;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static ru.yandex.realty.api.UnsafeOkHttpClient.getUnsafeOkHttpClient;

public class JSoupSteps extends TestWatcher {

    public static final String CONTENT = "content";
    public static final String TITLE_LOCATOR = "head title";
    public static final String DESCRIPTION_LOCATOR = "head meta[name='description']";
    public static final String H1_LOCATOR = "body h1";
    public static final String PAGE_404 = "Нет такой страницы";
    public static final String PAGE_500 = "Произошла ошибка";

    private Connection connection;

    @Getter
    Document webPage;

    @Inject
    private RealtyWebConfig config;

    @Step("Запрос >>>")
    public void get() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias("Получали урл")
                .pollInterval(1, SECONDS).atMost(config.getUrlTimeout(), SECONDS).ignoreExceptions()
                .until(() -> {
                    webPage = connection.get();
                    return true;
                });
    }

    @Step("Запрашиваем «{resource}»")
    public JSoupSteps connectTo(String resource) {
        connection = Jsoup.connect(resource)
                .sslSocketFactory(getUnsafeOkHttpClient().sslSocketFactory());
        return this;
    }

    @Step("Добавляем куку {name} = {value}")
    public JSoupSteps cookie(String name, String value) {
        connection.cookie(name, value);
        return this;
    }

    @Step("Добавляем хедер {name} = {value}")
    public JSoupSteps header(String name, String value) {
        connection.header(name, value);
        return this;
    }

    @Step("Хедер для мобильности")
    public JSoupSteps mobileHeader() {
        header("user-agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) " +
                        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1");
        return this;
    }

    @Step("Ищем «{locator}»")
    public Elements select(String locator) {
        return webPage.select(locator);
    }

    @Override
    protected void finished(Description description) {
        saveHead();
    }

    @Attachment(value = "HEAD", type = "text/plain")
    private String saveHead() {
        if (webPage != null) {
            return webPage.head().toString();
        }
        return "";
    }
}
