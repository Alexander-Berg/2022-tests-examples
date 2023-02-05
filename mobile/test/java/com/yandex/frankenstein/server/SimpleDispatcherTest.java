package com.yandex.frankenstein.server;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class SimpleDispatcherTest {

    private static final String BODY = "response body";
    private static final int RESPONSE_CODE = 42;

    private final RecordedRequest mRequest = mock(RecordedRequest.class);

    @Parameterized.Parameter
    public SimpleDispatcher simpleDispatcher;

    @Parameterized.Parameter(1)
    public int expectedCode;

    @Parameterized.Parameters(name = "response code = {1}")
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {new SimpleDispatcher(BODY), HttpURLConnection.HTTP_OK},
                {new SimpleDispatcher(BODY, RESPONSE_CODE), RESPONSE_CODE},
        });
    }

    @Test
    public void testDispatch() {
        final MockResponse response = simpleDispatcher.dispatch(mRequest);
        final String body = response.getBody().readUtf8();
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);

        assertThat(body).isEqualTo(BODY);
        assertThat(code).isEqualTo(expectedCode);
    }
}
