package ru.auto.tests.desktop.matchers;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.devtools.v101.network.model.Request;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static ru.auto.tests.desktop.consts.Metrics.PAGE_REF;
import static ru.auto.tests.desktop.consts.Metrics.PAGE_URL;

public class RequestHasQueryItemsMatcher extends TypeSafeMatcher<Request> {

    private final NameValuePair[] queryPairs;

    private RequestHasQueryItemsMatcher(NameValuePair... queryPairs) {
        this.queryPairs = queryPairs;
    }

    @Override
    protected boolean matchesSafely(Request request) {
        URI requestUri = URI.create(request.getUrl());
        List<NameValuePair> queries = URLEncodedUtils.parse(requestUri, String.valueOf(StandardCharsets.UTF_8));

        return hasItems(queryPairs).matches(queries);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has in query items ").appendValue(queryPairs);
    }

    @Factory
    public static RequestHasQueryItemsMatcher hasQuery(NameValuePair... queryPairs) {
        return new RequestHasQueryItemsMatcher(queryPairs);
    }

    @Factory
    public static RequestHasQueryItemsMatcher hasGoal(String goalUrl) {
        NameValuePair pageUrl = new BasicNameValuePair(PAGE_URL, goalUrl);

        return new RequestHasQueryItemsMatcher(pageUrl);
    }

    @Factory
    public static RequestHasQueryItemsMatcher hasGoal(String goalUrl, String url) {
        NameValuePair pageUrl = new BasicNameValuePair(PAGE_URL, goalUrl);
        NameValuePair pageRef = new BasicNameValuePair(PAGE_REF, url);

        return new RequestHasQueryItemsMatcher(pageUrl, pageRef);
    }
}
