package ru.yandex.webmaster3.viewer.http.robotstxt;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.util.collections.Cf;
import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.http.WebmasterErrorResponse;
import ru.yandex.webmaster3.core.robotstxt.AllowInfo;
import ru.yandex.webmaster3.core.robotstxt.AnalysisResult;
import ru.yandex.webmaster3.core.robotstxt.ErrorInfo;
import ru.yandex.webmaster3.core.robotstxt.FormatErrorType;
import ru.yandex.webmaster3.core.zora.GoZoraService;
import ru.yandex.webmaster3.core.zora.data.response.ZoraUrlFetchResponse;
import ru.yandex.webmaster3.storage.robotstxt.AnalyzeRobotsTxtService;
import ru.yandex.webmaster3.storage.robotstxt.model.AnalyzeRobotsTxtResult;
import ru.yandex.webmaster3.storage.util.W3DispatcherHttpService;

import static org.easymock.EasyMock.mock;

/**
 * User: azakharov
 * Date: 18.06.14
 * Time: 19:51
 */
public class AnalyzeRobotsTxtActionTest {
    @Test
    public void testNoHostNameNoRobotsTxt() throws Exception {
        final AnalyzeRobotsTxtAction action = new AnalyzeRobotsTxtAction();
        final W3DispatcherHttpService mockDispatcherHttpService = EasyMock.createMock(W3DispatcherHttpService.class);
        AnalyzeRobotsTxtService analyzeRobotsTxtService = new AnalyzeRobotsTxtService(mock(GoZoraService.class), mockDispatcherHttpService);
        action.setAnalyzeRobotsTxtService(analyzeRobotsTxtService);
        final AnalyzeRobotsTxtRequest request = new AnalyzeRobotsTxtRequest();
        try {
            action.process(request);
            Assert.fail();
        } catch (WebmasterException e) {
            Assert.assertTrue(e.getError() instanceof WebmasterErrorResponse.IllegalParameterValueResponse);
        }
    }

    @Test
    public void testMaxUrlsCountExceeded() throws Exception {

        final AnalyzeRobotsTxtAction action = new AnalyzeRobotsTxtAction();

        final W3DispatcherHttpService mockDispatcherHttpService = EasyMock.createMock(W3DispatcherHttpService.class);
        AnalyzeRobotsTxtService analyzeRobotsTxtService = new AnalyzeRobotsTxtService(mock(GoZoraService.class), mockDispatcherHttpService) {
            @Override
            protected ZoraUrlFetchResponse getRobotsTxtContent(URL host) throws WebmasterException {
                Assert.fail();
                return null;
            }
        };
        action.setAnalyzeRobotsTxtService(analyzeRobotsTxtService);

        final AnalyzeRobotsTxtRequest request = new AnalyzeRobotsTxtRequest();

        final StringBuilder urlsBuilder = new StringBuilder();
        for (int i = 0; i <= AnalyzeRobotsTxtService.MAX_URLS_COUNT; i++) {
            urlsBuilder.append('/').append(i).append('\n');
        }

        request.setUrls(urlsBuilder.toString());
        request.setRobotsTxtContent("/disallow");

        AnalyzeRobotsTxtResponse response = action.process(request);
        Assert.assertTrue(response instanceof AnalyzeRobotsTxtResponse.MainErrorResponse);
    }

    @Test
    public void testMaxRobotsTxtSize() throws Exception {
        final AnalyzeRobotsTxtAction action = new AnalyzeRobotsTxtAction();
        final W3DispatcherHttpService mockDispatcherHttpService = EasyMock.createMock(W3DispatcherHttpService.class);
        AnalyzeRobotsTxtService analyzeRobotsTxtService = new AnalyzeRobotsTxtService(mock(GoZoraService.class), mockDispatcherHttpService) {
            @Override
            protected ZoraUrlFetchResponse getRobotsTxtContent(URL host) throws WebmasterException {
                Assert.fail();
                return null;
            }
        };
        action.setAnalyzeRobotsTxtService(analyzeRobotsTxtService);

        final StringBuilder robotsTxtBuilder = new StringBuilder();
        final String template = "11112222333344445555666677778888\n";
        for (int i = 0; i <= 16000; i++) {
            robotsTxtBuilder.append(template);
        }

        final String robotsTxt = StringUtils.trimToEmpty(robotsTxtBuilder.toString());
        final String[] urls = new String[]{"/a"};

        AnalysisResult result = new AnalysisResult(
                Cf.list(new ErrorInfo(FormatErrorType.WARN_UNKNOWN_FIELD, 2), new ErrorInfo(FormatErrorType.WARN_UNKNOWN_FIELD, 3)),
                Cf.list(1l, 2l, 3l),
                Cf.list(new AllowInfo(true, "", false, "/a"))
        );
        EasyMock.expect(mockDispatcherHttpService.analyzeRobotsTxt(EasyMock.anyString(), EasyMock.anyObject(List.class))).andReturn(result);

        EasyMock.replay(mockDispatcherHttpService);

        final AnalyzeRobotsTxtRequest request = new AnalyzeRobotsTxtRequest();
        request.setRobotsTxtContent(robotsTxt);
        request.setUrls(urls[0]);

        final AnalyzeRobotsTxtResponse resp = action.process(request);
        Assert.assertTrue(resp instanceof AnalyzeRobotsTxtResponse.NormalResponse);
        AnalyzeRobotsTxtResponse.NormalResponse response = (AnalyzeRobotsTxtResponse.NormalResponse) resp;

        Assert.assertEquals(robotsTxt, response.getRobotsTxtContent());
        Assert.assertEquals(3, response.getParseErrors().size());
        final Iterator<AnalyzeRobotsTxtResult.RobotsTxtErrorInfo> it = response.getParseErrors().iterator();
        final AnalyzeRobotsTxtResult.RobotsTxtErrorInfo e1 = it.next();
        Assert.assertEquals(2, e1.getLine());
        Assert.assertEquals(FormatErrorType.WARN_UNKNOWN_FIELD, e1.getType());
        final AnalyzeRobotsTxtResult.RobotsTxtErrorInfo e2 = it.next();
        Assert.assertEquals(3, e2.getLine());
        Assert.assertEquals(FormatErrorType.WARN_UNKNOWN_FIELD, e2.getType());
        final AnalyzeRobotsTxtResult.RobotsTxtErrorInfo e3 = it.next();
        Assert.assertEquals(AnalyzeRobotsTxtService.MAX_ROBOTS_TXT_SIZE / template.length() + 1, e3.getLine());
        Assert.assertEquals(FormatErrorType.ERR_ROBOTS_HUGE, e3.getType());

        EasyMock.verify(mockDispatcherHttpService);
    }

    @Test
    public void testRobotsTxtWithErrors() throws Exception {
        final AnalyzeRobotsTxtAction action = new AnalyzeRobotsTxtAction();

        final W3DispatcherHttpService mockDispatcherHttpService = EasyMock.createMock(W3DispatcherHttpService.class);

        AnalyzeRobotsTxtService analyzeRobotsTxtService =
                new AnalyzeRobotsTxtService(mock(GoZoraService.class), mockDispatcherHttpService) {
                    @Override
                    protected ZoraUrlFetchResponse getRobotsTxtContent(URL host) throws WebmasterException {
                        Assert.fail();
                        return null;
                    }
                };
        action.setAnalyzeRobotsTxtService(analyzeRobotsTxtService);
        final String robotsTxt = StringUtils.trimToEmpty(
                "User-Agent: *\n" +
                        "Dissallow: /1/\n" +
                        "Diisallow: /2/\n"
        );
        final String[] urls = new String[]{"/a"};

        final AnalysisResult result = new AnalysisResult(
                Cf.list(new ErrorInfo(FormatErrorType.WARN_UNKNOWN_FIELD, 2), new ErrorInfo(FormatErrorType.WARN_UNKNOWN_FIELD, 3)),
                Cf.list(1l, 2l, 3l),
                Cf.list(new AllowInfo(true, "", false, "/a"))
        );
        EasyMock.expect(mockDispatcherHttpService.analyzeRobotsTxt(EasyMock.anyString(), EasyMock.anyObject(List.class))).andReturn(result);

        EasyMock.replay(mockDispatcherHttpService);

        final AnalyzeRobotsTxtRequest request = new AnalyzeRobotsTxtRequest();
        request.setRobotsTxtContent(robotsTxt);
        request.setUrls(urls[0]);

        final AnalyzeRobotsTxtResponse resp = action.process(request);
        Assert.assertTrue(resp instanceof AnalyzeRobotsTxtResponse.NormalResponse);
        AnalyzeRobotsTxtResponse.NormalResponse response = (AnalyzeRobotsTxtResponse.NormalResponse) resp;

        Assert.assertEquals(robotsTxt, response.getRobotsTxtContent());
        Assert.assertEquals(2, response.getParseErrors().size());
        final Iterator<AnalyzeRobotsTxtResult.RobotsTxtErrorInfo> it = response.getParseErrors().iterator();
        final AnalyzeRobotsTxtResult.RobotsTxtErrorInfo e1 = it.next();
        Assert.assertEquals(2, e1.getLine());
        Assert.assertEquals(FormatErrorType.WARN_UNKNOWN_FIELD, e1.getType());
        Assert.assertEquals("Dissallow: /1/", e1.getRule());
        final AnalyzeRobotsTxtResult.RobotsTxtErrorInfo e2 = it.next();
        Assert.assertEquals(3, e2.getLine());
        Assert.assertEquals(FormatErrorType.WARN_UNKNOWN_FIELD, e2.getType());
        Assert.assertEquals("Diisallow: /2/", e2.getRule());

        EasyMock.verify(mockDispatcherHttpService);
    }

    @Test
    public void testGetLine() {
        Assert.assertEquals("", AnalyzeRobotsTxtService.getLine("", 1));
        Assert.assertEquals(null, AnalyzeRobotsTxtService.getLine("", 2));
        Assert.assertEquals("1", AnalyzeRobotsTxtService.getLine("1", 1));
        Assert.assertEquals(null, AnalyzeRobotsTxtService.getLine("1", 2));

        for (String delim : Lists.newArrayList("\r", "\n", "\r\n")) {
            String text = "1" + delim;
            Assert.assertEquals("1", AnalyzeRobotsTxtService.getLine(text, 1));
            Assert.assertEquals("", AnalyzeRobotsTxtService.getLine(text, 2));
            Assert.assertEquals(null, AnalyzeRobotsTxtService.getLine(text, 3));

            text = "1" + delim + "2";
            Assert.assertEquals("1", AnalyzeRobotsTxtService.getLine(text, 1));
            Assert.assertEquals("2", AnalyzeRobotsTxtService.getLine(text, 2));
            Assert.assertEquals(null, AnalyzeRobotsTxtService.getLine(text, 3));
        }

        for (String delim2 : Lists.newArrayList("\r", "\n", "\r\n")) {
            for (String delim1 : Lists.newArrayList("\r", "\n", "\r\n")) {
                String text = "1" + delim1 + "2" + delim2;
                Assert.assertEquals("1", AnalyzeRobotsTxtService.getLine(text, 1));
                Assert.assertEquals("2", AnalyzeRobotsTxtService.getLine(text, 2));
                Assert.assertEquals("", AnalyzeRobotsTxtService.getLine(text, 3));
                Assert.assertEquals(null, AnalyzeRobotsTxtService.getLine(text, 4));
            }
        }
    }

    @Test
    public void testGetLine1() {
        //             cc nc rc r n r r n nc
        String text = "1\n2\r3\r\n\r\r\n\n77";

        Assert.assertEquals("1", AnalyzeRobotsTxtService.getLine(text, 1));
        Assert.assertEquals("2", AnalyzeRobotsTxtService.getLine(text, 2));
        Assert.assertEquals("3", AnalyzeRobotsTxtService.getLine(text, 3));
        Assert.assertEquals("", AnalyzeRobotsTxtService.getLine(text, 4));
        Assert.assertEquals("", AnalyzeRobotsTxtService.getLine(text, 5));
        Assert.assertEquals("", AnalyzeRobotsTxtService.getLine(text, 6));
        Assert.assertEquals("77", AnalyzeRobotsTxtService.getLine(text, 7));
    }
}
