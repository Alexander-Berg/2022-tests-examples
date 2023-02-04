package ru.yandex.qe.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * User: bgleb
 */
public class DefaultJsonMapperTest {

    @Test
    public void check_date_in_iso() throws IOException {
        final DateTime date = DateTime.parse("2013-10-10T20:50:10.266+08:00");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DefaultJsonMapper().writeValue(out, date);

        final DateTime expected = DateTime.parse("2013-10-10T12:50:10.266Z").toDateTime(DateTimeZone.getDefault());

        assertThat(new String(out.toByteArray()), equalTo('"' + expected.toString() + '"'));
    }

    @Test
    public void serialize_url() throws IOException {
        final URL url = new URL("https://www.yandex.ru");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DefaultJsonMapper().writeValue(out, url);

        final String expected = '"' + "https://www.yandex.ru" + '"';
        assertThat(new String(out.toByteArray()), equalTo(expected));
    }

    @Test
    public void deserialize_url() throws IOException {
        final URL url = new DefaultJsonMapper().readValue("\"http://www.yandex.ru\"", URL.class);

        assertThat(url, equalTo(new URL("http://www.yandex.ru")));
    }

    @Test
    public void serialize_uri() throws IOException {
        final URI uri = URI.create("ceph://s3-nirvana.qloud.yandex.net/nirvanatest/uzhos.txt");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DefaultJsonMapper().writeValue(out, uri);

        final String expected = '"' + "ceph://s3-nirvana.qloud.yandex.net/nirvanatest/uzhos.txt" + '"';
        assertThat(new String(out.toByteArray()), equalTo(expected));
    }

    @Test
    public void deserialize_uri() throws IOException {
        final URI uri = new DefaultJsonMapper().readValue("\"ceph://s3-nirvana.qloud.yandex.net/nirvanatest/uzhos.txt\"", URI.class);

        assertThat(uri, equalTo(URI.create("ceph://s3-nirvana.qloud.yandex.net/nirvanatest/uzhos.txt")));
    }
}
