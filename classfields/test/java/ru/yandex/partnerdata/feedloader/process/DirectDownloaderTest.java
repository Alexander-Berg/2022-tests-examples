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
 * Simple test for file downloading without any proxy
 *
 * @author sunlight
 */
@Ignore
public class DirectDownloaderTest {
    private static final Logger log = Logger.getLogger(ZoraDownloaderTest.class);

    private static final int URL_CONNECT_TIMEOUT = 120_000;
    private static final int WEBSOCKET_TIMEOUT = 120_000;
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 6_000_000;
    private static final int POOLED_CONNECTION_TIMEOUT = 6_000_000;
    private static final int MAX_CONNECTIONS = Integer.MAX_VALUE;
    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (compatible; YandexVerticals/1.0; +http://yandex.com/bots)";


    private static final int FUTURE_TIMOUT = 50;

    private final AsyncHttpClient directAsyncClient = new AsyncHttpClient(
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
                    build());

    private final static int adaptorESThreadCount = 1;
    private final static int QUEUE_SIZE = 5;
    private final ExecutorService adaptorES = Executors.newFixedThreadPool(adaptorESThreadCount,
            "Idle",
            new LinkedBlockingQueue<Runnable>(QUEUE_SIZE),
            true);

    @Test
    public void makeDirectRequest() throws Exception {
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
        final AsyncHttpClient.BoundRequestBuilder builder = directAsyncClient.prepareGet(url)
                .setHeader("Connection", "close");

        final Request request = builder.build();
        final ListenableFuture<DirectDownloaderTest.Result> future = directAsyncClient.executeRequest(request, new DirectDownloaderTest.SimpleFeedHandler());
        final DirectDownloaderTest.Result result = future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
        assert (result.content.length() > 0);
        log.debug("download completed" + result);
    }


    private static class SimpleFeedHandler implements AsyncHandler<DirectDownloaderTest.Result> {

        private DirectDownloaderTest.Status status;
        private final StringBuilder content = new StringBuilder();

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            status = DirectDownloaderTest.Status.FAIL;
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
                status = DirectDownloaderTest.Status.OK;
                return STATE.CONTINUE;
            } else {
                return STATE.ABORT;
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
        public DirectDownloaderTest.Result onCompleted() throws Exception {
            log.debug("download completed");
            return new DirectDownloaderTest.Result(status, content);
        }

        private boolean isStatusCodeOK(final int statusCode) {
            return statusCode == 200;
        }
    }

    private static class Result {
        private final DirectDownloaderTest.Status status;
        private final StringBuilder content;

        public Result(final DirectDownloaderTest.Status status,
                      final StringBuilder content) {
            this.status = status;
            this.content = content;
        }

        public DirectDownloaderTest.Status getStatus() {
            return status;
        }

        public StringBuilder getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "status=" + status +
                    '}';
        }
    }

    private enum Status {
        OK(0),
        FAIL(1);

        public final int id;

        private Status(final int id) {
            this.id = id;
        }
    }
}
