package ru.auto.tests.desktop.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.devtools.v101.network.model.Request;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static ru.auto.tests.desktop.consts.Metrics.CABINET_METRICS_URL;
import static ru.auto.tests.desktop.consts.Metrics.COMMON_METRICS_URL;

public class RequestsMatcher extends TypeSafeMatcher<List<Request>> {

    private int countFounded = 0;
    private final String url;
    private final Matcher<Request> requestMatcher;
    private final Matcher<Integer> countMatcher;

    public RequestsMatcher(String url, Matcher<Request> requestMatcher, Matcher<Integer> countMatcher) {
        this.url = url;
        this.requestMatcher = requestMatcher;
        this.countMatcher = countMatcher;
    }

    @Override
    protected boolean matchesSafely(List<Request> requests) {
        int count = 0;
        List<Request> filteredRequests = requests.stream()
                .filter(r -> r.getUrl().contains(url))
                .collect(Collectors.toList());

        for (Request request : filteredRequests) {
            boolean matches = requestMatcher.matches(request);

            if (matches) {
                count += 1;
            }
        }

        countFounded = count;
        return countMatcher.matches(count);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("contains ")
                .appendDescriptionOf(countMatcher)
                .appendText(" request with url ")
                .appendValue(url)
                .appendText(", that has ")
                .appendDescriptionOf(requestMatcher);
    }

    @Override
    protected void describeMismatchSafely(List<Request> requests, Description mismatchDescription) {
        mismatchDescription.appendText("contains ")
                .appendValue(countFounded)
                .appendText(" times in ")
                .appendValue(requests.size())
                .appendText(" requests");
    }

    @Factory
    @SafeVarargs
    public static RequestsMatcher onlyOneMetricsRequest(Matcher<Request>... matchers) {
        return new RequestsMatcher(COMMON_METRICS_URL, allOf(matchers), equalTo(1));
    }

    @Factory
    @SafeVarargs
    public static RequestsMatcher onlyOneCabinetMetricsRequest(Matcher<Request>... matchers) {
        return new RequestsMatcher(CABINET_METRICS_URL, allOf(matchers), equalTo(1));
    }

    @Factory
    public static RequestsMatcher onlyOneRequest(String url, Matcher<Request> matchers) {
        return new RequestsMatcher(url, matchers, equalTo(1));
    }

    @Factory
    public static RequestsMatcher requestsMatch(String url, Matcher<Request> matchers, Integer count) {
        return new RequestsMatcher(url, matchers, equalTo(count));
    }

    @Factory
    public static RequestsMatcher noRequest(Matcher<Request> matchers) {
        return new RequestsMatcher(COMMON_METRICS_URL, matchers, equalTo(0));
    }
}
