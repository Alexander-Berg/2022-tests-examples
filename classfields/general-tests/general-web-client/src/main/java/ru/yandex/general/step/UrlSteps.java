package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;
import ru.yandex.general.config.GeneralWebConfig;

import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertThat;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.yandex.general.matchers.UrlMatcher.hasNoDiffWithUrl;

public class UrlSteps extends WebDriverSteps {

    @Inject
    @Getter
    private GeneralWebConfig config;

    private List<UriDiffFilter> ignoringParams = newArrayList();

    private UriBuilder uriBuilder;

    public UrlSteps fromUri(String uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }

    public UrlSteps testing() {
        uriBuilder = UriBuilder.fromUri(config.getTestingURI());
        return this;
    }

    public UrlSteps production() {
        uriBuilder = UriBuilder.fromUri(config.getProductionURI());
        return this;
    }

    public UrlSteps realProduction() {
        uriBuilder = UriBuilder.fromUri(config.getRealProductionURI());
        return this;
    }

    public UrlSteps passport() {
        uriBuilder = UriBuilder.fromUri(config.getPassportProdURL().toString());
        return this;
    }

    public UrlSteps path(String path) {
        uriBuilder.path(path);
        return this;
    }

    public UrlSteps uri(String resource) {
        uriBuilder.uri(resource);
        return this;
    }

    public UrlSteps setProductionHost() {
        setCookie("font_loaded", "YSv1", config.getBaseDomain());
        uri(getCurrentUrl());
        uriBuilder.host(config.getProductionURI().getHost());
        return this;
    }

    @Step("Добавляем параметр «{name}» = «{value}» к билдеру")
    public UrlSteps queryParam(String name, String value) {
        uriBuilder.queryParam(name, value);
        return this;
    }

    @Step("Добавляем фрагмент «{name}» к билдеру")
    public UrlSteps fragment(String name) {
        uriBuilder.fragment(name);
        return this;
    }

    public UrlSteps open() {
        open(uriBuilder.build().toString());
        return this;
    }

    public UrlSteps login() {
        uriBuilder = UriBuilder.fromUri("http://aqua.yandex-team.ru/auth.html");
        return this;
    }

    @Step("Получаем текущий url")
    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    @Step("Получаем offerId")
    public String getOfferId() {
        String offerUrl = getDriver().getCurrentUrl();
        Pattern pattern = Pattern.compile("\\/((card)|(form)|(offer))\\/(.+)\\/");
        Matcher matcher = pattern.matcher(offerUrl);
        matcher.find();
        return matcher.group(5);
    }

    @Step("Должны быть на странице «{url}»")
    public UrlSteps shouldNotDiffWith(String url) {
        ignoreParam("explicit_russia_region");
        assertThat(getDriver(), hasNoDiffWithUrl(url, ignoringParams));
        return this;
    }

    public UrlSteps shouldNotDiffWithWebDriverUrl() {
        waitSomething(500, MILLISECONDS);
        shouldNotDiffWith(uriBuilder.build().toString());
        return this;
    }

    public String getBaseTestingUrl() {
        Pattern pattern = Pattern.compile("(https:\\/\\/)(.*\\.ru)(\\/?)");
        Matcher matcher = pattern.matcher(testing().toString());
        matcher.find();
        return matcher.group(2);
    }

    public String getPassportUrl() {
        String passportUrl;
        if (config.getTusEnviroment().equals("prod")) {
            passportUrl = config.getPassportProdURL().toString();
        } else {
            passportUrl = config.getPassportTestURL().toString();
        }
        return passportUrl;
    }

    @Step("Добавляем параметр {name} к списку игнорируемых")
    public UrlSteps ignoreParam(String name) {
        ignoringParams.add(param(name));
        return this;
    }

    public String toString() {
        return uriBuilder.build().toString();
    }

}
