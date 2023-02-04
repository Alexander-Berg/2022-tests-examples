package ru.yandex.partnerdata.feedloader.process;

import com.ning.http.client.*;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.util.concurrent.Executors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Simple test for file downloading with Zora proxy
 *
 * @author sunlight
 */
@Ignore
public class ZoraDownloaderTest {

    private static final Logger log = Logger.getLogger(ZoraDownloaderTest.class);

    private static final int URL_CONNECT_TIMEOUT = 120_000;
    private static final int WEBSOCKET_TIMEOUT = 120_000;
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 6_000_000;
    private static final int POOLED_CONNECTION_TIMEOUT = 6_000_000;
    private static final int ZORA_RESPONCE_TIMEOUT = 60;
    private static final int MAX_CONNECTIONS = Integer.MAX_VALUE;
    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (compatible; YandexVerticals/1.0; +http://yandex.com/bots)";
    // make tunnel to testing machine:
    // ssh -L 8166:zora.yandex.net:8166 <yandex-login>@back-nonrt-01-sas.test.vertis.yandex.net
    private static final String ZORA_HOST = "localhost";
    private static final int ZORA_PORT = 8166;
    private final ProxyServer zoraProxy = new ProxyServer(ZORA_HOST, ZORA_PORT);
    private final static String ZORA_SOURCE_NAME = "feedloader";
    private final static String HTTP_PROTOCOL = "http://";
    private final static String HTTPS_PROTOCOL = "https://";
    private static final int REDIRECT_DEPTH = 5;

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
                    setFollowRedirect(false).
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
        download("http://nasledie-don.ru/admin/upload/yandex.xml");
        download("https://zipal.ru/export/user/211095/YANDEX_all");
        download("https://stol-yar.ru/files/feed.gz");
        download("http://base.sohoestate.ru/export/yandex_commercial.php");
        download("http://agenton.ru/export/realty-yandex.xml.gz");
        download("http://сэкспертом.рф/feed.xml");
        download("https://status-realt.ru/resale/yandex.xml");
        download("https://capitalmars.com/agregators/yandex.php");
        download("http://homecrm.ru/unloadings/4/xml/yandex/yandex.xml");
        download("https://esbn.ru/unload/2ce3431b64493e9c2b582dcc0d4786b2/yandex/Feed_252_yan1");
        download("https://callexchange.ru/api/v2/feed?realty=tricolor-ya&token=ld1TVVAbfkynAl6hbzPy3B6wS8zRKToy");
        download("https://mayak01.ru/yrl/mayak.xml");
        download("http://domizdoma.ru/xml/feed_xml.xml");
        download("https://homecrm.ru/unloadings/1/xml/yandex/yandex.xml");
        download("http://region-msk.roomle.ru/xml/Yandex.xml");
        download("https://zipal.ru/export/agency/6649/YANDEX");
        download("https://zipal.ru/export/agency/6649/YANDEX_commercial");
        download("https://imls.ru/Files/Fids/2365/aea7328986294a15b8f389081976fd4d-YANDEX.xml");
        download("https://imls.ru/Files/Fids/7085/7f30ea167be54ceabde40c452b276b28-YANDEX.xml");
        download("https://imls.ru/Files/Fids/7202/c569f992a4444a99a74e557b4025c77e-YANDEX.xml");
        download("https://www.hirsh.ru/xml/yandex.xml");
        download("https://stroim-2002.ru/realty.php");
        download("https://zipal.ru/export/user/211095/YANDEX");
        download("https://imls.ru/Files/Fids/6519/c2462fbe3ec5465bb5fa14f35b543e3a-YANDEX.xml");
        download("https://zipal.ru/export/agency/6484/YANDEX_all");
        download("https://zipal.ru/export/user/109797/YANDEX_commercial");
        download("http://anlogos.ru/yandex_export.php");
        download("https://zipal.ru/export/agency/5596/YANDEX_all");
        download("http://man909.ru/feed/");
    }

    private void download(final String url) throws Exception {
        final ZoraDownloaderTest.Result result = download0(url, 0);
        assert (result.getContent().length() > 0);
    }

    private ZoraDownloaderTest.Result download0(final String url, final int redirectDepth) throws Exception {
        final boolean isUseZoraHttps = isUseZoraHttps(url);
        final AsyncHttpClient.BoundRequestBuilder builder = zoraAsyncClient.prepareGet(substituteProtocol(url, isUseZoraHttps))
                .setHeader("Connection", "close")
                .setHeader("X-Yandex-Sourcename", ZORA_SOURCE_NAME)
                .setHeader("X-Yandex-Response-Timeout", String.valueOf(ZORA_RESPONCE_TIMEOUT))
                .setHeader("X-Yandex-Use-Https", String.valueOf(isUseZoraHttps));

        final Request request = builder.build();
        final ListenableFuture<Result> future = zoraAsyncClient.executeRequest(request, new SimpleFeedHandler());

        Result result = future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
        if (result.getStatus() == Status.REDIRECT_NEEDED &&
                result.getLocation() != null &&
                redirectDepth < REDIRECT_DEPTH) {
            System.out.println("redirect needed from " + url + " to " + result.getLocation());
            result = download0(result.getLocation(), redirectDepth + 1);
            log.debug("download completed" + result);
        } else if (result.getStatus() == Status.REDIRECT_NEEDED &&
                (result.getLocation() == null &&
                        redirectDepth >= REDIRECT_DEPTH)) {
            log.debug("redirect failed for " + result.getLocation() + " depth: " + (redirectDepth + 1));
        }
        return result;
    }

    private boolean isUseZoraHttps(final String url) {
        return url.startsWith(HTTPS_PROTOCOL);
    }

    private String substituteProtocol(final String url, final boolean isUseZoraHttps) {
        if (isUseZoraHttps) {
            return url.replace(HTTPS_PROTOCOL, HTTP_PROTOCOL);
        } else {
            return url;
        }
    }

    private static class SimpleFeedHandler implements AsyncHandler<Result> {

        private Status status;
        private String location;
        private final StringBuilder content = new StringBuilder();

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            status = Status.FAIL;
            System.out.println(t.getMessage());
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            final String chunk = new String(bodyPart.getBodyPartBytes());
            System.out.println(chunk);
            content.append(chunk);
            return STATE.CONTINUE;
        }

        @Override
        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
            final int statusCode = responseStatus.getStatusCode();
            System.out.println("statusCode: " + statusCode);

            if (isStatusCodeOK(statusCode)) {
                status = Status.OK;
                return STATE.CONTINUE;
            } else if (isStatusRedirect(statusCode)) {
                status = Status.REDIRECT_NEEDED;
                return STATE.CONTINUE;
            } else {
                return STATE.ABORT;
            }
        }

        @Override
        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
            final FluentCaseInsensitiveStringsMap map = headers.getHeaders();
            location = headers.getHeaders().getFirstValue("Location");

            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                System.out.println("header: " + entry.getKey());

                for (String value : entry.getValue()) {
                    System.out.println("  value: " + value);
                }
            }
            return STATE.CONTINUE;
        }

        @Override
        public Result onCompleted() throws Exception {
            log.debug("download completed");
            return new Result(status, location, content);
        }

        private boolean isStatusCodeOK(final int statusCode) {
            return statusCode == 200;
        }

        private boolean isStatusRedirect(final int statusCode) {
            return statusCode == 301 || statusCode == 302;
        }

    }

    private static class Result {
        private final Status status;
        private final String location;
        private final StringBuilder content;

        public Result(Status status, String location, StringBuilder content) {
            this.status = status;
            this.location = location;
            this.content = content;
        }

        public Status getStatus() {
            return status;
        }

        public String getLocation() {
            return location;
        }

        public StringBuilder getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "status=" + status +
                    ", location='" + location + '\'' +
                    '}';
        }
    }

    private enum Status {
        OK(0),
        FAIL(1),
        REDIRECT_NEEDED(2);

        public final int id;

        private Status(final int id) {
            this.id = id;
        }
    }
}
