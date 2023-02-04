package ru.yandex.partnerdata.feedloader.process;

import com.ning.http.client.*;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.concurrent.Executors;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Tests chunked downloading from Zora with usage of range in header for big feed
 *
 * @author sunlight
 */
@Ignore
public class BigFeedZoraContentRangeDownloadTest {

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


    private final Semaphore semaphore = new Semaphore(1);
    private final static int adaptorESThreadCount = 1;
    private final static int QUEUE_SIZE = 5;
    private final ExecutorService adaptorES = Executors.newFixedThreadPool(adaptorESThreadCount,
            "Idle",
            new LinkedBlockingQueue<Runnable>(QUEUE_SIZE),
            true);


    @Test
    public void makeZoraRequest() throws Exception {
        final String path = "/Users/sunlight/feed/rabota_yandex_vacancy_all_referral.xml.gz";
        final String url = "http://public.superjob.ru/export/rabota_yandex_vacancy_all_referral.xml.gz";
        final int chunckSize = 40000;

        final long totalLength = getContentLength(url);
        System.out.println("total feed length: " + totalLength);

        final long startTime = System.currentTimeMillis();
        for (int start = 0; start <= totalLength; start += chunckSize) {
            final int end = start + chunckSize - 1;
            System.out.println("loading chunk from " + start + " to " + end);
            final AsyncHttpClient.BoundRequestBuilder builder = zoraAsyncClient.prepareGet(url)
                    .setHeader("Connection", "close").
                            setHeader("X-Yandex-Sourcename", ZORA_SOURCE_NAME).
                            setHeader("range", "bytes=" + start + "-" + end);

            final Request request = builder.build();
            semaphore.tryAcquire();
            final ListenableFuture<String> future = zoraAsyncClient.executeRequest(request, new BigFeedZoraContentRangeDownloadTest.SimpleFeedHandler(path));
            try {
                future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                System.out.println("execution exception" + e);
            } catch (InterruptedException e) {
                System.out.println("interrupted exception" + e);
            } catch (TimeoutException e) {
                System.out.println("timeout exception" + e);
            } finally {
                semaphore.release();
            }
        }
        final long endTime = System.currentTimeMillis();
        System.out.println("time:  " + (endTime - startTime));
    }

    private final long getContentLength(final String url) {
        final Request request = zoraAsyncClient.prepareGet(url)
                .setHeader("Connection", "close").
                        setHeader("X-Yandex-Sourcename", ZORA_SOURCE_NAME).
                        build();
        final ListenableFuture<Long> future = zoraAsyncClient.executeRequest(request, new BigFeedZoraContentRangeDownloadTest.ContentLengthHandler());
        semaphore.tryAcquire();
        try {
            return future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            System.out.println("execution exception" + e);
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            System.out.println("interrupted exception" + e);
            throw new IllegalStateException(e);
        } catch (TimeoutException e) {
            System.out.println("timeout exception" + e);
            throw new IllegalStateException(e);
        } finally {
            semaphore.release();
        }
    }


    private class ContentLengthHandler implements AsyncHandler<Long> {

        private long contentLength;

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            System.out.println(t.getMessage());
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            return STATE.ABORT;
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

            contentLength = Long.valueOf(map.getFirstValue("Content-Length"));
            return STATE.CONTINUE;
        }

        @Override
        public Long onCompleted() throws Exception {
            return contentLength;
        }

        private boolean isStatusCodeOK(final int statusCode) {
            return statusCode == 200 || statusCode == 206;
        }
    }

    private class SimpleFeedHandler implements AsyncHandler<String> {

        final private String path;
        final private File feedFile;
        final private OutputStream os;

        public SimpleFeedHandler(final String path) throws FileNotFoundException, IOException {
            this.path = path;
            this.feedFile = new File(path);
            feedFile.createNewFile();
            this.os = new BufferedOutputStream(new FileOutputStream(feedFile, true));
        }

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            System.out.println(t.getMessage());
            try {
                os.close();
            } catch (IOException e) {
                System.out.println("e = " + e.getStackTrace());
            }
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            final byte[] part = bodyPart.getBodyPartBytes();
            os.write(part);
            System.out.println(" byte part size of " + part.length + " received");
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
        public String onCompleted() {
            try {
                os.close();
            } catch (IOException e) {
                System.out.println("e = " + e.getStackTrace());
            }
            return "Ok";
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
