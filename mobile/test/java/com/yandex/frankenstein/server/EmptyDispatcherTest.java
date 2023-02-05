package com.yandex.frankenstein.server;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class EmptyDispatcherTest {

    private static final int RESPONSE_CODE = 42;

    private final EmptyDispatcher mEmptyDispatcher = new EmptyDispatcher(RESPONSE_CODE);
    private final RecordedRequest mRequest = mock(RecordedRequest.class);

    @Test
    public void testPrepareResponse() {
        final MockResponse response = mEmptyDispatcher.dispatch(mRequest);
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);
        final Buffer body = response.getBody();

        assertThat(code).isEqualTo(RESPONSE_CODE);
        assertThat(body).isNull();
    }
}
