package ru.yandex.partnerdata.feedloader.process;

import com.ning.http.client.*;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import ru.yandex.common.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Tests chunked downloading from Zora with usage of range in header
 *
 * @author sunlight
 */
@Ignore
public class ZoraContentRangeDownloaderTest {

    private static final int URL_CONNECT_TIMEOUT = 120_000;
    private static final int WEBSOCKET_TIMEOUT = 120_000;
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 6_000_000;
    private static final int POOLED_CONNECTION_TIMEOUT = 6_000_000;
    private static final int MAX_CONNECTIONS = Integer.MAX_VALUE;
    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (compatible; YandexVerticals/1.0; +http://yandex.com/bots)";
    // make tunnel to testing machine:
    // ssh -L 8166:zora.yandex.net:8166 <yandex-login>@back-nonrt-01-sas.test.vertis.yandex.net
    private static final String ZORA_HOST = "localhost";
    private static final int ZORA_PORT = 8166;
    private final ProxyServer zoraProxy = new ProxyServer(ZORA_HOST, ZORA_PORT);
    private final static String ZORA_SOURCE_NAME = "feedloader";

    private static final int FUTURE_TIMOUT = 50;

    private final AsyncHttpClient zoraAsyncClient = new AsyncHttpClient(
            new AsyncHttpClientConfig.Builder().
                    setConnectTimeout(URL_CONNECT_TIMEOUT).
                    setWebSocketTimeout(WEBSOCKET_TIMEOUT).
                    setRequestTimeout(REQUEST_TIMEOUT_IN_MILLIS).
                    setUserAgent(USER_AGENT_VALUE).
                    setIOThreadMultiplier(1).
                    setMaxConnections(MAX_CONNECTIONS).
                    setPooledConnectionIdleTimeout(POOLED_CONNECTION_TIMEOUT).
                    setAllowPoolingConnections(true).
                    setAcceptAnyCertificate(true).
                    setFollowRedirect(true).
                    setProxyServer(zoraProxy).
                    build());

    private final static int adaptorESThreadCount = 1;
    private final static int QUEUE_SIZE = 5;
    private final ExecutorService adaptorES = Executors.newFixedThreadPool(adaptorESThreadCount,
            "Idle",
            new LinkedBlockingQueue<Runnable>(QUEUE_SIZE),
            true);


    @Test
    public void makeZoraRequest() throws Exception {
        final String url = "http://public.realtysystems.ru/xml/export/yandex/z7Bn68TSQK8hGaEB2hEE_commerce.xml";
        final int totalSize = 2000;
        final int chunckSize = 1000;

        final StringBuffer totalFeedContent = new StringBuffer();
        for (int start = 0; start <= totalSize; start += chunckSize) {
            final int end = start + chunckSize - 1;
            System.out.println("loading chunk from " + start + " to " + end);
            final AsyncHttpClient.BoundRequestBuilder builder = zoraAsyncClient.prepareGet(url)
                    .setHeader("Connection", "close").
                            setHeader("X-Yandex-Sourcename", ZORA_SOURCE_NAME).
                            setHeader("range", "bytes=" + start + "-" + end);

            final Request request = builder.build();
            final ListenableFuture<StringBuffer> future = zoraAsyncClient.executeRequest(request, new SimpleFeedHandler());

            future.addListener(
                    new Runnable() {
                        public void run() {
                            try {
                                final StringBuffer result = future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
                                totalFeedContent.append(result);
                            } catch (ExecutionException e) {
                                System.out.println("execution exception" + e);
                            } catch (InterruptedException e) {
                                System.out.println("interrupted exception" + e);
                            } catch (TimeoutException e) {
                                System.out.println("timeout exception" + e);
                            }
                        }
                    }, adaptorES
            );
            Thread.sleep(5000);
        }
        System.out.println("totalFeedContent = " + totalFeedContent);
        System.out.println("validate xml..");
        try {
            assert (validateXml(totalFeedContent.toString()));
        } catch (ParserConfigurationException e) {
            System.out.println("e = " + e);
        }
        System.out.println("xml validate ok");

        System.out.println("make one request to download all content");
        final StringBuffer contentDownloadedByOneRequest = new StringBuffer();
        final AsyncHttpClient.BoundRequestBuilder builder = zoraAsyncClient.prepareGet(url)
                .setHeader("Connection", "close").
                        setHeader("X-Yandex-Sourcename", ZORA_SOURCE_NAME);
        final Request request = builder.build();
        final ListenableFuture<StringBuffer> future = zoraAsyncClient.executeRequest(request, new SimpleFeedHandler());
        future.addListener(
                new Runnable() {
                    public void run() {
                        try {
                            final StringBuffer result = future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
                            contentDownloadedByOneRequest.append(result);
                        } catch (ExecutionException e) {
                            System.out.println("execution exception" + e);
                        } catch (InterruptedException e) {
                            System.out.println("interrupted exception" + e);
                        } catch (TimeoutException e) {
                            System.out.println("timeout exception" + e);
                        }
                    }
                }, adaptorES
        );
        Thread.sleep(5000);
        assert (contentDownloadedByOneRequest.toString().equals(totalFeedContent.toString()));
    }

    private boolean validateXml(final String feedContent) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();

        builder.setErrorHandler(new SimpleErrorHandler());

        try {
            builder.parse(new InputSource(new ByteArrayInputStream(feedContent.getBytes())));
        } catch (SAXException e) {
            System.out.println("broken xml");
            System.out.println("e = " + e);
            return false;
        } catch (IOException e) {
            System.out.println("broken xml");
            System.out.println("e = " + e);
            return false;
        }
        return true;
    }

    private class SimpleFeedHandler implements AsyncHandler<StringBuffer> {

        final private StringBuffer buffer;

        public SimpleFeedHandler() {
            this.buffer = new StringBuffer();
        }

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            System.out.println(t.getMessage());
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            final byte[] part = bodyPart.getBodyPartBytes();
            buffer.append(new String(part));
            return STATE.CONTINUE;
        }

        @Override
        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
            final int statusCode = responseStatus.getStatusCode();
            System.out.println("statusCode: " + statusCode);
            if (!isStatusCodeOK(statusCode)) {
                return STATE.ABORT;
            } else {
                return STATE.CONTINUE;
            }
        }

        @Override
        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
            final FluentCaseInsensitiveStringsMap map = headers.getHeaders();

            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                System.out.println("header: " + entry.getKey());

                for (String value : entry.getValue()) {
                    System.out.println("  value: " + value);
                }
            }
            return STATE.CONTINUE;
        }

        @Override
        public StringBuffer onCompleted() {
            return buffer;
        }

        private boolean isStatusCodeOK(final int statusCode) {
            return statusCode == 200 || statusCode == 206;
        }
    }

    public class SimpleErrorHandler implements ErrorHandler {
        public void warning(SAXParseException e) throws SAXException {
            System.out.println(e.getMessage());
            throw e;
        }

        public void error(SAXParseException e) throws SAXException {
            System.out.println(e.getMessage());
            throw e;
        }

        public void fatalError(SAXParseException e) throws SAXException {
            System.out.println(e.getMessage());
            throw e;
        }
    }
}