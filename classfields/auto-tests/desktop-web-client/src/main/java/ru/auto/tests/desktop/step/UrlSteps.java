package ru.auto.tests.desktop.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.openqa.selenium.WebDriverException;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.desktop.DesktopConfig;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.desktop.matchers.UrlMatcher.hasNoDiffWithUrl;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;

/**
 * @author kurau (Yuri Kalinin)
 */
public class UrlSteps extends WebDriverSteps {

    private UriBuilder uriBuilder;

    private List<UriDiffFilter> ignoringParams = new ArrayList<>();

    @Getter
    @Inject
    private DesktopConfig config;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private BasePageSteps basePageSteps;

    public UrlSteps fromUri(String uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }

    public UrlSteps testing() {
        uriBuilder = UriBuilder.fromUri(config.getTestingURI());
        ignoringParams = newArrayList();
        return this;
    }

    public UrlSteps autoruProdURI() {
        uriBuilder = UriBuilder.fromUri(config.getAutoruProdURI());
        ignoringParams = newArrayList();
        return this;
    }

    public UrlSteps subdomain(String subdomain) {
        uriBuilder = UriBuilder.fromUri(format("https://%s.%s/", subdomain, config.getBaseDomain()));
        ignoringParams = newArrayList();
        return this;
    }

    @Step("???????????????? ?? ???????????? (?? ?????????? - ???????????????? ???????? _branch, ?? ???????????????? - ?????????????????????? ???????? _branch=production")
    public UrlSteps setProduction() {
        if (config.getBaseDomain().equals(config.getTestDomain())) {
            cookieSteps.setCookieForBaseDomain(config.getBranchCookieName(), "production");
        } else {
            cookieSteps.deleteCookie(config.getBranchCookieName());
        }

        return this;
    }

    public UrlSteps autoruDomain() {
        uriBuilder = UriBuilder.fromUri(config.getAutoruDomain());
        ignoringParams = newArrayList();
        return this;
    }

    public UrlSteps desktopURI() {
        uriBuilder = UriBuilder.fromUri(config.getDesktopURI());
        ignoringParams = newArrayList();
        return this;
    }

    public UrlSteps mobileURI() {
        uriBuilder = UriBuilder.fromUri(config.getMobileURI());
        ignoringParams = newArrayList();
        return this;
    }

    public UrlSteps path(String path) {
        uriBuilder.path(path);
        return this;
    }

    public UrlSteps fragment(String fragment) {
        uriBuilder.fragment(fragment);
        return this;
    }

    public UrlSteps withSchema(String schema) {
        uriBuilder.scheme(schema);
        return this;
    }

    @Step("?????????????????? ???????????????? x-real-ip = {value} ?? ??????????????")
    public UrlSteps addXRealIP(String value) {
        uriBuilder.queryParam("x-real-ip", value);
        return this;
    }

    @Step("?????????????????? ???????????????? {name} = {value} ?? ??????????????")
    public UrlSteps addParam(String name, String value) {
        uriBuilder.queryParam(name, value);
        return this;
    }

    public UrlSteps pathsAndParams(String pathsAndParams) {
        URI uri = UriBuilder.fromUri(pathsAndParams).build();
        List<NameValuePair> queryList = URLEncodedUtils.parse(uri, String.valueOf(StandardCharsets.UTF_8));

        path(uri.getPath());
        queryList.forEach(query ->
                addParam(query.getName(), query.getValue())
        );

        return this;
    }

    @Step("???????????????? ???????????????? {name} = {value}")
    public UrlSteps replaceParam(String name, String value) {
        uriBuilder.replaceQueryParam(name, value);
        return this;
    }

    @Step("???????????????? query ?????????????? ???? {query}")
    public UrlSteps replaceQuery(String query) {
        uriBuilder.replaceQuery(query);
        return this;
    }

    public UrlSteps onCurrentUrl() {
        uriBuilder = UriBuilder.fromUri(getDriver().getCurrentUrl());
        return this;
    }

    public UrlSteps fromAutotest() {
        addParam("from", "autotest");
        return this;
    }

    @Step("?????????????????? ???????????????? {name} ?? ???????????? ????????????????????????")
    public UrlSteps ignoreParam(String name) {
        ignoringParams.add(param(name));
        return this;
    }

    public UrlSteps ignoreParams(String... name) {
        asList(name).forEach(n -> ignoringParams.add(param(n)));
        return this;
    }

    public UrlSteps open() {
        try {
            open(uriBuilder.build().toString());
        } catch (WebDriverException e) {
            refresh();
        }

        hideDevtoolsBranch();

        return this;
    }

    public void refresh() {
        getDriver().navigate().refresh();
        hideDevtoolsBranch();
    }

    @Step("???????????? ???????? ???? ??{url}??")
    public UrlSteps shouldNotDiffWith(String url) {
        await().pollInterval(1, SECONDS).atMost(10, SECONDS)
                .ignoreExceptions().untilAsserted(() -> assertThat("???? ???????????????? ?????????????????? ??????", getDriver(),
                        hasNoDiffWithUrl(url).setFilters(ignoringParams)));
        return this;
    }

    @Step("???????????? ???????? ???? ?????????? ??{m}??")
    public UrlSteps shouldSeeHost(Matcher m) {
        await().pollInterval(1, SECONDS).atMost(10, SECONDS)
                .untilAsserted(() -> assertThat(format("?????????????? ???????? ???????????? ???????? ??%s??", m),
                        new URL(getDriver().getCurrentUrl()).getHost(), m));
        return this;
    }

    @Step("???????????? ???????? ?????????????? {expectedCount} ????????/??????????????")
    public void shouldSeeCertainNumberOfTabs(int expectedCount) {
        await().pollInterval(1, SECONDS).atMost(7, SECONDS)
                .ignoreExceptions().untilAsserted(() -> Assertions.assertThat(getDriver().getWindowHandles())
                        .describedAs("???? ?????????????????? ?????????? ??????????????").hasSize(expectedCount));
    }

    public UrlSteps shouldNotSeeDiff() {
        return shouldNotDiffWith(uriBuilder.build().toString());
    }

    @Step("?????????????????? url {matcher}")
    public void shouldUrl(Matcher<String> matcher, long timeout) {
        Awaitility.await().pollInterval(1L, TimeUnit.SECONDS).atMost(timeout, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertThat(this.getDriver().getCurrentUrl(), matcher);
        });
    }

    @Step("???????????????? ?????????????? url")
    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    public String toString() {
        return uriBuilder.build().toString();
    }

    public void hideDevtoolsBranch() {
        if (!config.getBranchCookieValue().equals("")) {
            basePageSteps.hideElement(basePageSteps.onBasePage().branch());
        }
    }

}

