package ru.yandex.general.matchers;

import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.List;

import static java.lang.String.format;
import static ru.yandex.general.utils.Utils.removeExplitRussiaRegionParam;

public class HarEntryMatchers {

    public static Matcher<HarEntry> hasQueryString(Matcher matcher) {
        return new TypeSafeDiagnosingMatcher<HarEntry>() {
            @Override
            protected boolean matchesSafely(HarEntry harEntry, Description description) {
                return matcher.matches(harEntry.getRequest().getQueryString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Должны видеть URL с параметрами ").appendDescriptionOf(matcher);
            }
        };
    }

    public static Matcher<HarEntry> hasPageRef(String url) {
        return new TypeSafeDiagnosingMatcher<HarEntry>() {
            boolean matched = false;

            @Override
            protected boolean matchesSafely(HarEntry harEntry, Description description) {
                List<HarNameValuePair> queries = harEntry.getRequest().getQueryString();
                for (HarNameValuePair query : queries) {
                    if (query.getName().equals("page-ref")) {
                        String actual = removeExplitRussiaRegionParam(query.getValue());
                        if (actual.equals(removeExplitRussiaRegionParam(url)))
                            matched = true;
                    }
                }
                return matched;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(format("Должны видеть URL с pageRef = «%s»", url));
            }
        };
    }

    public static Matcher<HarEntry> hasRequestPostData(Matcher matcher) {
        return new TypeSafeDiagnosingMatcher<HarEntry>() {
            @Override
            protected boolean matchesSafely(HarEntry harEntry, Description description) {
                return harEntry.getRequest().getPostData() != null && matcher.matches(harEntry.getRequest()
                        .getPostData().getText());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Должны видеть Request Payload ").appendDescriptionOf(matcher);
            }
        };
    }

    public static Matcher<HarEntry> hasRequestParamValue(Matcher matcher) {
        return new TypeSafeDiagnosingMatcher<HarEntry>() {
            @Override
            protected boolean matchesSafely(HarEntry harEntry, Description description) {
                return harEntry.getRequest().getPostData() != null &&
                        harEntry.getRequest().getPostData().getParams() != null &&
                        matcher.matches(harEntry.getRequest().getPostData().getParams().get(0).getValue());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Должны видеть Request Payload ").appendDescriptionOf(matcher);
            }
        };
    }

}
