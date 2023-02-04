package ru.yandex.qe.http.handler;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.util.Throwables;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.qe.http.handler.FluentResponseHandler.defaultHandler;
import static ru.yandex.qe.http.handler.FluentResponseHandler.newHandler;
import static ru.yandex.qe.util.Throwables.rethrowing;

/**
 * @author rurikk
 */
public class FluentResponseHandlerTest {
    @Test
    public void okString() throws Exception {
        assertThat(defaultHandler()
                        .returnString()
                        .handleResponse(ok("content")),
                is("content"));
    }

    @Test
    public void okList() throws Exception {
        assertThat(defaultHandler()
                        .returnStringList()
                        .handleResponse(ok("line1\nline2")),
                is(Lists.newArrayList("line1", "line2")));
    }

    @Test
    public void okJson() throws Exception {
        assertThat(defaultHandler()
                        .returnJson(SimpleObj.class)
                        .handleResponse(ok("{\"x\":123}")),
                is(new SimpleObj(123)));
    }

    @Test
    public void okStream() throws Exception {
        assertThat(defaultHandler()
                        .processStream(this::streamToString)
                        .handleResponse(ok("ok")),
                is("ok"));
    }

    @Test
    public void okStream2() throws Exception {
        String[] holder = new String[1];
        defaultHandler()
                .processStream(rethrowing(is -> {
                    holder[0] = streamToString(is);
                }))
                .handleResponse(ok("ok"));
        assertThat(holder[0], is("ok"));
    }

    @Test
    public void testReader() throws Exception {
        Map<String, String> map = defaultHandler().returning(HttpResponseReader::jsonStringMap)
                .handleResponse(ok("{\"a\": \"aa\"}"));
        assertThat(map, is(Collections.singletonMap("a", "aa")));
    }

    @Test
    public void testReader2() throws Exception {
        byte[] bytes = defaultHandler().returning(HttpResponseReader::asBytes)
                .handleResponse(ok("12"));
        assertThat(bytes, is("12".getBytes(UTF_8)));
    }

    @Test
    public void recovery() throws Exception {
        assertThat(newHandler()
                        .on4xx().recover(r -> "recovered")
                        .returnString()
                        .handleResponse(fail(404)),
                is("recovered"));
    }

    @Test
    public void failRun() throws Exception {
        String[] holder = new String[1];
        newHandler()
                .on5xx().run(() -> holder[0] = "log")
                .handleResponse(fail(505));
        assertThat(holder[0], is("log"));
    }

    @Test
    public void failThrow() throws Exception {
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            newHandler()
                    .on5xx().doThrow(() -> new IllegalArgumentException("fail"))
                    .handleResponse(fail(505));
        });
        assertThat(exception.getMessage(), is("fail"));

    }

    @Test
    public void failThrowFn() throws Exception {
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            newHandler()
                    .on5xx().doThrow(sl -> new IllegalArgumentException("" + sl.getStatusCode()))
                    .handleResponse(fail(505));
        });
        assertThat(exception.getMessage(), is("505"));
    }

    @Test
    public void failThrowFull() throws Exception {
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            newHandler()
                    .on5xx().doThrowFull(r -> new IllegalArgumentException(r.asString()))
                    .handleResponse(fail(505));
        });
        assertThat(exception.getMessage(), is("error"));
    }

    @Test
    public void failRules() throws Exception {
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            newHandler()
                    .onNon2xx().doThrow(sl -> new IllegalArgumentException("!200"))
                    .on4xx().doThrow(sl -> new IllegalArgumentException("4xx"))
                    .handleResponse(fail(401));
        });
        assertThat(exception.getMessage(), is("!200"));
    }

    private HttpResponse fail(int code) throws UnsupportedEncodingException {
        BasicHttpResponse ok = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, code, "fail"));
        ok.setEntity(new StringEntity("error"));
        return ok;
    }

    private HttpResponse ok(String s) throws Exception {
        BasicHttpResponse ok = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        ok.setEntity(new StringEntity(s));
        return ok;
    }

    private String streamToString(InputStream is) {
        try {
            return IOUtils.toString(is);
        } catch (Exception e) {
            throw Throwables.rethrow(e);
        }
    }

    public static class SimpleObj {
        int x;

        public SimpleObj() {
        }

        public SimpleObj(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleObj simpleObj = (SimpleObj) o;
            return x == simpleObj.x;
        }

        @Override
        public int hashCode() {
            return x;
        }
    }
}