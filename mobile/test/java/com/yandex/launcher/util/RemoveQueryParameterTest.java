package com.yandex.launcher.util;

import static org.hamcrest.MatcherAssert.assertThat;

import android.net.Uri;

import com.yandex.launcher.common.loaders.UrlUtils;
import com.yandex.launcher.BaseRobolectricTest;

import org.hamcrest.core.Is;
import org.junit.Test;

public class RemoveQueryParameterTest extends BaseRobolectricTest {

    public RemoveQueryParameterTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void uriHasQpQpRemoved() {
        Uri uri = Uri.parse("http://?qp=abc");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://"));
    }

    @Test
    public void uriHasQpQpRemoved2() {
        Uri uri = new Uri.Builder().scheme("http")
                .appendQueryParameter("qp", "abc").build();

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http:"));
    }

    @Test
    public void uriHasQpQpRemoved3() {
        Uri uri = Uri.parse("http://www.yandex.ru/?qp=abc");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://www.yandex.ru/"));
    }

    @Test
    public void uriHasQpQpRemoved4() {
        Uri uri = Uri.parse("http://www.yandex.ru?qp=abc");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://www.yandex.ru"));
    }

    @Test
    public void uriHasQpQpRemoved5() {
        Uri uri = Uri.parse("http://www.yandex.ru/qp?qp=abc");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://www.yandex.ru/qp"));
    }

    @Test
    public void uriHasManyQpOnlyTargetQpRemoved() {
        Uri uri = Uri.parse("http://?qp=abc&blc=jeioj");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://?blc=jeioj"));
    }

    @Test
    public void uriHasManyQpOnlyTargetQpRemoved2() {
        Uri uri = Uri.parse("http://?blc=jeioj&qp=abc");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://?blc=jeioj"));
    }

    @Test
    public void uriHasManyQpOnlyTargetQpRemoved3() {
        Uri uri = new Uri.Builder().scheme("http")
                .appendQueryParameter("qp", "abc")
                .appendQueryParameter("blc", "jeioj").build();

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http:?blc=jeioj"));
    }

    @Test
    public void uriHasNoQpNothingRemoved() {
        Uri uri = new Uri.Builder().scheme("http")
                .appendQueryParameter("qp", "abc")
                .appendQueryParameter("blc", "jeioj").build();

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http:?blc=jeioj"));
    }

    @Test
    public void uriHasEmptyQpQpRemoved() {
        Uri uri = new Uri.Builder().scheme("http")
                .appendQueryParameter("qp", null).build();

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http:"));
    }

    @Test
    public void uriHasEmptyQpQpRemoved3() {
        Uri uri = Uri.parse("http://?qp=");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://"));
    }

    @Test
    public void uriHasNoTargetQpNothingRemoved() {
        Uri uri = Uri.parse("http://?blc=jeioj&qvbp=abc");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://?qvbp=abc&blc=jeioj"));
    }

    @Test
    public void uriHasNoTargetQpNothingRemoved2() {
        Uri uri = new Uri.Builder().scheme("http")
                .appendQueryParameter("qvbp", "abc")
                .appendQueryParameter("blc", "jeioj").build();

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http:?qvbp=abc&blc=jeioj"));
    }

    @Test
    public void uriHasPathWithSameNameNothingRemoved() {
        Uri uri = new Uri.Builder().scheme("http")
                .path("qp")
                .appendQueryParameter("qvbp", "abc")
                .appendQueryParameter("blc", "jeioj").build();

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http:/qp?qvbp=abc&blc=jeioj"));
    }

    @Test
    public void uriHasPathWithSameNameNothingRemoved2() {
        Uri uri = Uri.parse("http://qp?qvbp=abc&blc=jeioj");

        uri = UrlUtils.removeQueryParameter(uri, "qp");

        assertThat(uri.toString(), Is.is("http://qp?qvbp=abc&blc=jeioj"));
    }
}
