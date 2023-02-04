package ru.yandex.wmtools.common.util.http;

import java.io.Reader;
import java.io.StringBufferInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class HttpUtilsTest {
    private static final String HTTP_CONTENT =
            "HTTP/1.1 200 OK\r\n" +
            "Server: nginx/1.4.1\r\n" +
            "Date: Tue, 27 Aug 2013 15:10:17 GMT\r\n" +
            "Content-Type: text/html\r\n" +
            "Content-Length: 319\r\n" +
            "Last-Modified: Thu, 22 Aug 2013 16:01:39 GMT\r\n" +
            "Connection: keep-alive\r\n" +
            "ETag: \"521635e3-13f\"\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "\r\n" +
            "<!DOCTYPE html>\n<html>\n" +
            "<head>\n" +
            "<title>Welcome to nginx!</title>\n" +
            "<style>\n" +
            "    body {\n" +
            "        width: 35em;\n" +
            "        margin: 0 auto;\n" +
            "        font-family: Tahoma, Verdana, Arial, sans-serif;\n" +
            "    }\n" +
            "</style>\n" +
            "<meta name='yandex-verification' content='51400c664bf2c174' >\n" +
            "</head>\n" +
            "<body>\n" +
            "<h1>Welcome to nginx!</h1>\n" +
            "</body>\n" +
            "</html>\n";

    @Test
    public void testParse() throws Exception {
        HttpResponse httpResponse = HttpUtils.parse(new StringBufferInputStream(HTTP_CONTENT));
        Assert.assertNotNull("Http parser doesn't found http entity", httpResponse.getEntity());
        String content = IOUtils.toString(httpResponse.getEntity().getContent());
        Assert.assertEquals("Wrong http content length", 319, content.length());
    }

    @Test
    public void testGetResponseContent() throws Exception {
        HttpResponse httpResponse = HttpUtils.parse(new StringBufferInputStream(HTTP_CONTENT));
        boolean gzipEncoded = HttpUtils.isGzipEncoded(httpResponse);
        httpResponse = HttpUtils.parse(new StringBufferInputStream(HTTP_CONTENT));
        Reader contentReader = HttpUtils.getResponseContent(httpResponse, gzipEncoded);
        String content = IOUtils.toString(contentReader);
        Assert.assertEquals("Wrong content length", 319, content.length());
    }
}
