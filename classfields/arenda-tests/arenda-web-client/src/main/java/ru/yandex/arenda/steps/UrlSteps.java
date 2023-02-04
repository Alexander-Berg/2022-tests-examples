package ru.yandex.arenda.steps;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.extension.context.LocatorStorage;
import ru.auto.tests.commons.extension.context.StepContext;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;
import ru.yandex.arenda.config.ArendaWebConfig;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.yandex.arenda.lambdas.WatchException.watchException;
import static ru.yandex.arenda.matcher.UrlMatcher.hasNoDiffWithUrl;
import static ru.yandex.arenda.matcher.WaitForMatcherDecorator.withWaitFor;

/**
 * @author kantemirov
 */
public class UrlSteps extends WebDriverSteps {

    public static final String DEGRADATION_ROLE_PARAM = "_degradation_role";
    public static final String ADMIN_ROLE_VALUE = "ADMIN_ROLE";
    public static final String ROLE_PARAM = "_role";
    public static final String ADMIN_READ_ONLY_VALUE = "ADMIN_READ_ONLY";
    public static final String CONCIERGE_MANAGER_EXTENDED_ROLE_VALUE = "CONCIERGE_MANAGER_EXTENDED_ROLE";
    public static final String CONCIERGE_MANAGER_ROLE_VALUE = "CONCIERGE_MANAGER_ROLE";

    @Inject
    @Getter
    private ArendaWebConfig config;

    @Inject
    private LocatorStorage locatorStorage;

    private UriBuilder uriBuilder;
    private List<UriDiffFilter> ignoringParams = newArrayList();


    public UrlSteps fromUri(String uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }

    public UrlSteps path(String path) {
        uriBuilder.path(path);
        return this;
    }

    public UrlSteps replacePath(String path) {
        uriBuilder.replacePath(path);
        return this;
    }

    @Step("Добавляем параметр {name} = {value} к билдеру")
    public UrlSteps queryParam(String name, String... values) {
        uriBuilder.queryParam(name, values);
        return this;
    }

    @Step("Добавляем параметр {name} к списку игнорируемых")
    public UrlSteps ignoreParam(String name) {
        ignoringParams.add(param(name));
        return this;
    }

    public UrlSteps testing() {
        uriBuilder = UriBuilder.fromUri(config.getTestingURI());
        return this;
    }

    public UrlSteps setProductionHost() {
        uriBuilder.host(config.getProductionURI().getHost());
        return this;
    }

    public UrlSteps setMobileProductionHost() {
        uriBuilder.host(config.getProductionURI().getHost().replace("realty.", "m.realty."));
        return this;
    }

    public void waitForUrl(String url, int timeout) {
        watchException(() -> given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await()
                .alias(format("Должен быть урл: «%s»", url))
                .ignoreExceptions().pollInterval(1, SECONDS).atMost(timeout, SECONDS)
                .until(() -> getCurrentUrl(), containsString(url)));
    }

    public UrlSteps open() {
        open(uriBuilder.build().toString());
        return this;
    }

    @Step("Должны быть на странице «{url}»")
    public UrlSteps shouldNotDiffWith(String url) {
        try {
            locatorStorage.getStepsList()
                    .add(new StepContext().setAction("urlMatcher").setDescription(hasNoDiffWithUrl(url, ignoringParams)
                            .toString()));
        } catch (Exception e) {
            //
        }
        assertThat(getDriver(), withWaitFor(hasNoDiffWithUrl(url, ignoringParams)));
        return this;
    }

    public UrlSteps shouldNotDiffWithWebDriverUrl() {
        shouldNotDiffWith(uriBuilder.build().toString());
        return this;
    }

    public UrlSteps login() {
        uriBuilder = UriBuilder.fromUri("http://aqua.yandex-team.ru/auth.html");
        return this;
    }

    public UrlSteps logout() {
        uriBuilder = UriBuilder.fromUri("https://passport.yandex.ru/passport");
        return this;
    }

    @Step("Open url «{url}»")
    public void open(String url) {
        getDriver().get(url);
    }

    @Step("Получаем текущий url")
    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    public String toString() {
        return uriBuilder.build().toString();
    }
}
