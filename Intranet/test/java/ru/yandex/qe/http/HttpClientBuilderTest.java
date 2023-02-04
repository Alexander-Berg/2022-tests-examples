package ru.yandex.qe.http;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.Resources;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.qe.http.aspects.HttpAspect;
import ru.yandex.qe.http.aspects.HttpAspectFactory;
import ru.yandex.qe.http.aspects.log.LogHttpAspectFactory;
import ru.yandex.qe.http.retries.RetryStrategy;
import ru.yandex.qe.testing.web.TestingWebServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 30.06.13
 * Time: 11:35
 */
public class HttpClientBuilderTest {

    @Test
    public void add_retry_strategy_bad_response() throws Exception {
        new TestingWebServer("/test", new HttpServlet() {
            int attempNumber = 0;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                attempNumber++;
                if (attempNumber == 1) {
                    throw new RuntimeException();
                } else if (attempNumber == 2) {
                    throw new IOException("bla bla");
                } else if (attempNumber == 3) {
                    resp.sendError(500);
                } else {
                    resp.setStatus(200);
                }
            }
        }).startUpRunShutdown(new Runnable() {
            @Override
            public void run() {
                final RetryStrategy mockedRetryStrategy = Mockito.mock(RetryStrategy.class);
                final HttpClient build = new HttpClientBuilder().setRetryStrategy(mockedRetryStrategy).build();

                when(mockedRetryStrategy.getRetryInterval()).thenReturn(10l);
                when(mockedRetryStrategy.retryRequest(argThat(new HttpResponseCodeMatcher(500)),
                        anyInt(), any(HttpContext.class))).thenReturn(true);

                when(mockedRetryStrategy.retryRequest(argThat(new HttpResponseCodeMatcher(200)),
                        anyInt(), any(HttpContext.class))).thenReturn(false);

                try {
                    final HttpResponse response = build.execute(
                            new HttpGet(String.format("http://localhost:%s/test", TestingWebServer.DEFAULT_PORT)));
                    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

                    verify(mockedRetryStrategy, atLeastOnce()).getRetryInterval();
                    verify(mockedRetryStrategy, times(4)).retryRequest(any(HttpResponse.class), anyInt(), any(HttpContext.class));
                    verifyNoMoreInteractions(mockedRetryStrategy);
                } catch (IOException e) {
                    Assertions.fail();
                }
            }
        });
    }


    @Test
    public void add_retry_strategy_server_down() {
        final RetryStrategy mockedRetryStrategy = Mockito.mock(RetryStrategy.class);
        final HttpClient build = new HttpClientBuilder().setRetryStrategy(mockedRetryStrategy).build();

        when(mockedRetryStrategy.retryRequest(any(IOException.class),
                anyInt(), any(HttpContext.class))).thenAnswer(new Answer<Object>() {
            int count = 0;

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                count++;
                if (count < 5) {
                    return true;
                }
                return false;
            }
        });

        try {
            build.execute(new HttpGet(String.format("http://localhost:%s/test", TestingWebServer.DEFAULT_PORT)));
            Assertions.fail();
        } catch (IOException ex) {
            verify(mockedRetryStrategy, times(5)).retryRequest(any(IOException.class), anyInt(), any(HttpContext.class));
            verifyNoMoreInteractions(mockedRetryStrategy);
        }
    }

    @Test
    public void add_aspect_good_response() throws Exception {
        new TestingWebServer("/test", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            }
        }).startUpRunShutdown(new Runnable() {
            @Override
            public void run() {
                final HttpAspectFactory mockedAspectFactory = Mockito.mock(HttpAspectFactory.class);
                final HttpAspect mockedAspect = Mockito.mock(HttpAspect.class);

                when(mockedAspectFactory.getAspect()).thenReturn(mockedAspect);

                final HttpClient build = new HttpClientBuilder().addAspectFactory(mockedAspectFactory).build();

                try {
                    final HttpResponse response = build.execute(
                            new HttpGet(String.format("http://localhost:%s/test", TestingWebServer.DEFAULT_PORT)));
                    assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
                    verify(mockedAspect).beforeInvoke(
                            any(HttpHost.class), any(HttpRequest.class), nullable(HttpContext.class));
                    verify(mockedAspect).afterInvoke(
                            any(HttpResponse.class), any(HttpHost.class), any(HttpRequest.class), nullable(HttpContext.class));
                    verifyNoMoreInteractions(mockedAspect);
                } catch (IOException e) {
                    Assertions.fail();
                }
            }
        });
    }

    @Test
    public void add_aspect_failed_response() {
        final HttpAspectFactory mockedAspectFactory = Mockito.mock(HttpAspectFactory.class);
        final HttpAspect mockedAspect = Mockito.mock(HttpAspect.class);

        when(mockedAspectFactory.getAspect()).thenReturn(mockedAspect);

        final HttpClient build = new HttpClientBuilder().addAspectFactory(mockedAspectFactory).build();

        try {
            build.execute(new HttpGet(String.format("http://localhost:%s/test", TestingWebServer.DEFAULT_PORT)));
            Assertions.fail();
        } catch (IOException e) {
            verify(mockedAspect, atLeastOnce()).beforeInvoke(
                    any(HttpHost.class), any(HttpRequest.class), nullable(HttpContext.class));
            verify(mockedAspect, atLeastOnce()).onFail(
                    any(IOException.class), any(HttpHost.class), any(HttpRequest.class), nullable(HttpContext.class));
            verifyNoMoreInteractions(mockedAspect);
        }
    }

    @Test
    public void add_log_aspect() {
        final HttpClient build = new HttpClientBuilder().addAspectFactory(new LogHttpAspectFactory(Collections.emptyList())).build();
        try {
            build.execute(new HttpGet(String.format("http://localhost:%s/test", TestingWebServer.DEFAULT_PORT)));
            Assertions.fail();
        } catch (IOException e) {
        }
    }

    @Test
    public void clientAuth() throws IOException {
        new HttpClientBuilder()
                .setClientAuthentication(file("client-1.key"), file("client-1.cer"))
                .build();
    }

    private File file(String resourceName) {
        return new File(Resources.getResource(resourceName).getPath());
    }
}
