package com.yandex.mail.react;

import org.junit.Test;

import java.util.Set;

import static com.yandex.mail.util.CollectionUtil.unmodifiableSetOf;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

public final class ReactWebViewTest {

    @Test
    public void isRequestAllowedShouldAllowRequestFromAllowedSetWithOneUrl() {
        String url = "http://alloweddomain/someResource";
        Set<String> allowedUrls = unmodifiableSetOf("http://alloweddomain/someResource");
        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isTrue();
    }

    @Test
    public void isRequestAllowedShouldAllowRequestFromAllowedSetWithSeveralUrls() {
        String url = "http://alloweddomain/someResource";

        Set<String> allowedUrls = unmodifiableSetOf(
                "http://domain1/resource1/",
                "http://domain2/resource2",
                "http://alloweddomain/someResource"
        );

        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isTrue();
    }

    @Test
    public void isRequestAllowedShouldDenyRequestToFileWithoutAllowedUrls() {
        String url = "file://some_file";
        Set<String> allowedUrls = emptySet();
        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isFalse();
    }

    @Test
    public void isRequestAllowedShouldDenyRequestToFileWithSomeAllowedUrls() {
        String url = "file://some_file";

        Set<String> allowedUrls = unmodifiableSetOf(
                "http://someurl",
                "http://someurl2",
                "file://somepath"
        );

        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isFalse();
    }

    @Test
    public void isRequestAllowedShouldAllowRequestToFileIfItIsInAllowedUrls() {
        String url = "file://some_file";

        Set<String> allowedUrls = unmodifiableSetOf(
                "http://someurl",
                "file://some_file",
                "http://someurl2"
        );

        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isTrue();
    }

    @Test
    public void isRequestAllowedShouldDenyRequestToContentProviderWithoutAllowedUrls() {
        String url = "content://authority/resource";
        Set<String> allowedUrls = emptySet();
        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isFalse();
    }

    @Test
    public void isRequestAllowedShouldDenyRequestToContentProviderWithSomeAllowedUrls() {
        String url = "content://authority/resource";

        Set<String> allowedUrls = unmodifiableSetOf(
                "http://someurl",
                "http://someurl2",
                "file://somepath"
        );

        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isFalse();
    }

    @Test
    public void isRequestAllowedShouldAllowRequestToContentProviderIfItIsInAllowedUrls() {
        String url = "content://authority/resource";

        Set<String> allowedUrls = unmodifiableSetOf(
                "http://someurl",
                "content://authority/resource",
                "http://someurl2"
        );

        String baseFakeUrl = "http://somefakeurl";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isTrue();
    }

    @Test
    public void isRequestAllowedShouldDenyRequestWithBaseFakeUrlIfItIsNotInAllowedUrls() {
        String url = "http://base_fake_url/resource1";

        Set<String> allowedUrls = unmodifiableSetOf(
                "http://base_fake_url/resource2",
                "http://someurl",
                "http://someurl2"
        );

        String baseFakeUrl = "http://base_fake_url";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isFalse();
    }

    @Test
    public void isRequestAllowedShouldAllowRequestToThirdPartlyUrl() {
        String url = "http://somedomain/img.jpg";
        Set<String> allowedUrls = emptySet();
        String baseFakeUrl = "http://base_fake_url";

        assertThat(ReactWebView.isRequestAllowed(url, allowedUrls, baseFakeUrl))
                .isTrue();
    }

    @Test
    public void urlShouldBeHandledByWebView_FileUrl() {
        String url = "file://whatever.jpg";
        assertThat(ReactWebView.urlShouldBeHandledByWebView(url)).isTrue();
    }

    @Test
    public void urlShouldBeHandledByWebView_CidUrl() {
        String url = "cid://whatever.jpg";
        assertThat(ReactWebView.urlShouldBeHandledByWebView(url)).isTrue();
    }

    @Test
    public void urlShouldBeHandledByWebView_HttpUrl() {
        String url = "http://whatever.jpg";
        assertThat(ReactWebView.urlShouldBeHandledByWebView(url)).isTrue();
    }

    @Test
    public void urlShouldBeHandledByWebView_HttpsUrl() {
        String url = "https://whatever.jpg";
        assertThat(ReactWebView.urlShouldBeHandledByWebView(url)).isTrue();
    }
}
