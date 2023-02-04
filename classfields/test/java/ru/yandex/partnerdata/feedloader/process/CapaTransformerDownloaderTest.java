package ru.yandex.partnerdata.feedloader.process;

import com.ning.http.client.*;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.util.concurrent.Executors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static ru.yandex.feedloader.utils.UrlUtils.addProtocolToUrl;

/**
 * Simple test for file downloading with capa-transformer
 *
 * @author sunlight
 */
@Ignore
public class CapaTransformerDownloaderTest {

    private static final Logger log = Logger.getLogger(ZoraDownloaderTest.class);

    private static final int URL_CONNECT_TIMEOUT = 120_000;
    private static final int WEBSOCKET_TIMEOUT = 120_000;
    private static final int REQUEST_TIMEOUT_IN_MILLIS = 6_000_000;
    private static final int POOLED_CONNECTION_TIMEOUT = 6_000_000;
    private static final int MAX_CONNECTIONS = Integer.MAX_VALUE;
    private static final String USER_AGENT_VALUE = "Mozilla/5.0 (compatible; YandexVerticals/1.0; +http://yandex.com/bots)";

    public final static String CAPA_TRANSFORMER_API_TEMPLATE_NAME = "${capa-transformer}";
    public final static String capaTransformerApiUrl = "back-nonrt-01-sas.test.vertis.yandex.net:36550";
    private final static String HTTP_PROTOCOL = "http://";
    private final static String HTTPS_PROTOCOL = "https://";

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
    public void makeCapaTransformerRequest() throws Exception {
        download("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market");
        download("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market");
        download("http://${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market");
        download("http://${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market");
        download("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Fautoprice.ru%2Fautoru_xmlPart%2F292359.xml&model=Offer&charset=UTF-8&format=Avtoru");
        download("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Fautoprice.ru%2Fautoru_xmlPart%2F292359.xml&model=Offer&charset=UTF-8&format=Avtoru");
        download("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Fxn--2-7sbaaee0bza6adset.xn--p1ai%2Fpricelist.xml&model=Offer&charset=UTF-8&format=Market");
        download("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Fxn--2-7sbaaee0bza6adset.xn--p1ai%2Fpricelist.xml&model=Offer&charset=UTF-8&format=Market");
        download("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Fforauto.su%2Fprice%2Fautoru-export.zip&model=Offer&charset=UTF-8&compress=zip&format=Avtoru");
        download("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Fforauto.su%2Fprice%2Fautoru-export.zip&model=Offer&charset=UTF-8&compress=zip&format=Avtoru");
        download("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Fforder.ru%2Fimages%2Fprice%2F1-autoru.csv&model=Offer&charset=cp1251&format=AutoruCsv");
        download("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Fforder.ru%2Fimages%2Fprice%2F1-autoru.csv&model=Offer&charset=cp1251&format=AutoruCsv");
        download("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Fforder.ru%2Fimages%2Fprice%2F1-autoru.csv&model=Offer&charset=cp1251&format=AutoruCsv");
        download("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Fforder.ru%2Fimages%2Fprice%2F1-autoru.csv&model=Offer&charset=cp1251&format=AutoruCsv");
    }

    @Test
    public void checkTemplateSubstitutionCorrectness() {
        assert(check("${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "http://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "http://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("http://${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "http://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("http://${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "http://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("http://${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "http://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("https://${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "https://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("https://${capa-transformer}/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "https://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
        assert(check("http://${capa-transformer}api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market",
                "http://back-nonrt-01-sas.test.vertis.yandex.net:36550/api/1.x/stream/parts?url=http%3A%2F%2Ftorens-auto.com%2Fbitrix%2Fcatalog_export%2Fimport_all.php&model=Offer&charset=UTF-8&format=Market"));
    }

    private boolean check(final String urlBeforeSubstitution, final String urlAfterSubstitution) {
        return addProtocolToUrl(substituteUrlTemplate(urlBeforeSubstitution)).equals(urlAfterSubstitution);
    }

    private void download(final String url) throws Exception {
        final AsyncHttpClient.BoundRequestBuilder builder = directAsyncClient.prepareGet(
                addProtocolToUrl(substituteUrlTemplate(url)))
                .setHeader("Connection", "close");

        final Request request = builder.build();
        final ListenableFuture<CapaTransformerDownloaderTest.Result> future =
                directAsyncClient.executeRequest(request, new CapaTransformerDownloaderTest.SimpleFeedHandler());
        final CapaTransformerDownloaderTest.Result result = future.get(FUTURE_TIMOUT, TimeUnit.MINUTES);
        assert (result.content.length() > 0);
        log.debug("download completed" + result);
        Thread.sleep(5000);
    }

    protected String substituteUrlTemplate(String url) {
        return url
                .replace(CAPA_TRANSFORMER_API_TEMPLATE_NAME + "/", capaTransformerApiUrl + "/")
                .replace(CAPA_TRANSFORMER_API_TEMPLATE_NAME, capaTransformerApiUrl + "/");
    }

    private static class SimpleFeedHandler implements AsyncHandler<CapaTransformerDownloaderTest.Result> {

        private CapaTransformerDownloaderTest.Status status;
        private final StringBuilder content = new StringBuilder();

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            status = CapaTransformerDownloaderTest.Status.FAIL;
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
                status = CapaTransformerDownloaderTest.Status.OK;
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
        public CapaTransformerDownloaderTest.Result onCompleted() throws Exception {
            log.debug("download completed");
            return new CapaTransformerDownloaderTest.Result(status, content);
        }

        private boolean isStatusCodeOK(final int statusCode) {
            return statusCode == 200;
        }
    }

    private static class Result {
        private final CapaTransformerDownloaderTest.Status status;
        private final StringBuilder content;

        public Result(final CapaTransformerDownloaderTest.Status status,
                      final StringBuilder content) {
            this.status = status;
            this.content = content;
        }

        public CapaTransformerDownloaderTest.Status getStatus() {
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
