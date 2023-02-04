package ru.yandex.wmtools.common.service;

import java.io.IOException;
import java.util.Locale;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.wmtools.common.data.xmlsearch.LinksCountRequest;

/**
 * @author aherman
 */
public class IndexInfoServiceTest {
    @Before
    public void setUp() throws Exception {
        //LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        //lc.reset();
        //BasicConfigurator.configureDefaultContext();
    }

    @Test
    public void testExtractLinksCount() throws Exception {
        IndexInfoService indexInfoService = new IndexInfoService();
        final CloseableHttpClient httpClient = EasyMock.createMock(CloseableHttpClient.class);

        XmlSearchService xmlSearchService = new XmlSearchService() {
            @Override
            protected CloseableHttpClient getHttpClient() {
                return httpClient;
            }
        };
        xmlSearchService.setLogRawXmlResponse(true);
        indexInfoService.setXmlSearchService(xmlSearchService);

        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "Ok");
        response.setEntity(new InputStreamEntity(this.getClass().getClassLoader().getResourceAsStream("WMCON_6147.xml")));
        EasyMock.expect(httpClient.execute(EasyMock.anyObject(HttpUriRequest.class)))
                .andReturn(new MockCloseableHttpResponse(response));
        indexInfoService.setXmlSearchService(xmlSearchService);

        LinksCountRequest request = new LinksCountRequest("blok.lviv.ua");
        EasyMock.replay(httpClient);
        Long result = indexInfoService.extractLinksCount(request);
        EasyMock.verify(httpClient);
        Assert.assertNotNull(result);
        Assert.assertEquals(6, result.intValue());
    }

    private static class MockCloseableHttpResponse implements CloseableHttpResponse {
        private final HttpResponse response;

        private MockCloseableHttpResponse(HttpResponse response) {
            this.response = response;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public StatusLine getStatusLine() {
            return response.getStatusLine();
        }

        @Override
        public void setStatusLine(StatusLine statusline) {
            response.setStatusLine(statusline);
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code) {
            response.setStatusLine(ver, code);
        }

        @Override
        public void setStatusLine(ProtocolVersion ver, int code, String reason) {
            response.setStatusLine(ver, code, reason);
        }

        @Override
        public void setStatusCode(int code) throws IllegalStateException {
            response.setStatusCode(code);
        }

        @Override
        public void setReasonPhrase(String reason) throws IllegalStateException {
            response.setReasonPhrase(reason);
        }

        @Override
        public HttpEntity getEntity() {
            return response.getEntity();
        }

        @Override
        public void setEntity(HttpEntity entity) {
            response.setEntity(entity);
        }

        @Override
        public Locale getLocale() {
            return response.getLocale();
        }

        @Override
        public void setLocale(Locale loc) {
            response.setLocale(loc);
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return response.getProtocolVersion();
        }

        @Override
        public boolean containsHeader(String name) {
            return response.containsHeader(name);
        }

        @Override
        public Header[] getHeaders(String name) {
            return response.getHeaders(name);
        }

        @Override
        public Header getFirstHeader(String name) {
            return response.getFirstHeader(name);
        }

        @Override
        public Header getLastHeader(String name) {
            return response.getLastHeader(name);
        }

        @Override
        public Header[] getAllHeaders() {
            return response.getAllHeaders();
        }

        @Override
        public void addHeader(Header header) {
            response.addHeader(header);
        }

        @Override
        public void addHeader(String name, String value) {
            response.addHeader(name, value);
        }

        @Override
        public void setHeader(Header header) {
            response.setHeader(header);
        }

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public void setHeaders(Header[] headers) {
            response.setHeaders(headers);
        }

        @Override
        public void removeHeader(Header header) {
            response.removeHeader(header);
        }

        @Override
        public void removeHeaders(String name) {
            response.removeHeaders(name);
        }

        @Override
        public HeaderIterator headerIterator() {
            return response.headerIterator();
        }

        @Override
        public HeaderIterator headerIterator(String name) {
            return response.headerIterator(name);
        }

        @Override
        @Deprecated
        public HttpParams getParams() {
            return response.getParams();
        }

        @Override
        @Deprecated
        public void setParams(HttpParams params) {
            response.setParams(params);
        }
    }
}
