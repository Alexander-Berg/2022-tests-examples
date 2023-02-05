package com.yandex.frankenstein.server;

import com.yandex.frankenstein.CallbacksDispatcher;
import com.yandex.frankenstein.CommandCaseResponse;
import com.yandex.frankenstein.CommandRegistry;
import com.yandex.frankenstein.CommandsProvider;
import com.yandex.frankenstein.ResultsProvider;
import com.yandex.frankenstein.Uri;
import com.yandex.frankenstein.settings.CommandSettings;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")
public class CommandDispatcherTest {

    private static final String CASE_PATH = "/case";
    private static final String COMMAND_PATH = "/command";
    private static final String RESULT_PATH = "/result";
    private static final String CALLBACK_PATH = "/callback";
    private static final String UNKNOWN_PATH = "/unknown";

    private static final String LAST_COMMAND_ID_KEY = "lastCommandId";
    private static final String COMMAND_ID_KEY = "commandId";
    private static final String COMMAND_ERROR_KEY = "commandError";
    private static final String COMMAND_ID = "command_id";
    private static final String LISTENER_ID = "listener_id";
    private static final String CALLBACK = "callback";
    private static final String ERROR = "some_error";

    private final CommandCaseResponse mCommandCaseResponse = mock(CommandCaseResponse.class);
    private final CommandSettings mCommandSettings = mock(CommandSettings.class);

    private final JSONObject mRequestJson = new JSONObject().put("request_key", "request_value");
    private final JSONObject mRequestWithErrorJson = new JSONObject().put(COMMAND_ERROR_KEY, ERROR);
    private final JSONObject mCallbackResult = new JSONObject().put("callback_key", "callback_value");
    private final JSONObject mCaseResponseBody = new JSONObject().put("case_key", "case_value");
    private final JSONObject mCommand = new JSONObject().put("command_key", "command_value");
    private final List<JSONObject> mCommands = Collections.singletonList(mCommand);
    private final JSONObject mCommandResponseBody = new JSONObject().put("commands", new JSONArray(mCommands));

    private final CommandDispatcher mCommandResponsePreparer =
            new CommandDispatcher(mCommandCaseResponse, mCommandSettings);

    private final RecordedRequest mRequest = mock(RecordedRequest.class);
    private final Buffer mRequestBody = mock(Buffer.class);
    private final Runnable mCaseRequestListener = mock(Runnable.class);

    private final CommandsProvider mCommandsProvider = mock(CommandsProvider.class);
    private final ResultsProvider mResultsProvider = mock(ResultsProvider.class);
    private final CallbacksDispatcher mCallbacksDispatcher = mock(CallbacksDispatcher.class);

    @Before
    public void setUp() {
        when(mCommandCaseResponse.toJsonString()).thenReturn(mCaseResponseBody.toString());

        when(mCommandSettings.getCasePath()).thenReturn(CASE_PATH);
        when(mCommandSettings.getRequestPath()).thenReturn(COMMAND_PATH);
        when(mCommandSettings.getResultPath()).thenReturn(RESULT_PATH);
        when(mCommandSettings.getCallbackPath()).thenReturn(CALLBACK_PATH);

        when(mRequestBody.readByteArray()).thenReturn(mRequestJson.toString().getBytes(Charset.defaultCharset()));
        when(mRequest.getBody()).thenReturn(mRequestBody);

        when(mCommandsProvider.get(anyString())).thenReturn(mCommands);

        doAnswer(invocation -> {
            final JSONObject result = invocation.getArgument(3);
            result.keySet().forEach(result::remove);
            mCallbackResult.keySet().forEach(key -> result.put(key, mCallbackResult.get(key)));
            return null;
        }).when(mCallbacksDispatcher).notify(anyString(), anyString(), any(), any());

        CommandRegistry.registerInstance(null, mCommandsProvider, mResultsProvider, mCallbacksDispatcher);
    }

    @After
    public void tearDown() {
        CommandRegistry.clearInstance();
    }

    @Test
    public void testDispatchCaseRequestWithCaseRequestListener() {
        final String url = newUriBuilder(CASE_PATH).build().toString();
        when(mRequest.getPath()).thenReturn(url);

        mCommandResponsePreparer.setCaseRequestListener(mCaseRequestListener);
        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final String body = response.getBody().readUtf8();

        assertThat(body).isEqualTo(mCaseResponseBody.toString());
        verify(mCaseRequestListener).run();
    }

    @Test
    public void testDispatchCaseRequestWithoutCaseRequestListener() {
        final String url = newUriBuilder(CASE_PATH).build().toString();
        when(mRequest.getPath()).thenReturn(url);

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final String body = response.getBody().readUtf8();

        assertThat(body).isEqualTo(mCaseResponseBody.toString());
        verifyZeroInteractions(mCaseRequestListener);
    }

    @Test
    public void testDispatchCommandRequest() {
        final String url = newUriBuilder(COMMAND_PATH)
                .appendQueryParameter(LAST_COMMAND_ID_KEY, COMMAND_ID).build().toString();
        when(mRequest.getPath()).thenReturn(url);

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final String body = response.getBody().readUtf8();

        verify(mCommandsProvider).get(COMMAND_ID);
        assertThat(body).isEqualTo(mCommandResponseBody.toString());
    }

    @Test
    public void testDispatchCommandRequestWithoutCommands() {
        final String url = newUriBuilder(COMMAND_PATH)
                .appendQueryParameter(LAST_COMMAND_ID_KEY, COMMAND_ID).build().toString();
        when(mRequest.getPath()).thenReturn(url);
        when(mCommandsProvider.get(COMMAND_ID)).thenReturn(Collections.emptyList());

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);

        verify(mCommandsProvider).get(COMMAND_ID);
        assertThat(code).isEqualTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
    }

    @Test
    public void testDispatchResultRequest() {
        final String url = newUriBuilder(RESULT_PATH)
                .appendQueryParameter(COMMAND_ID_KEY, COMMAND_ID).build().toString();
        when(mRequest.getPath()).thenReturn(url);

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);

        verify(mResultsProvider).put(eq(COMMAND_ID), refEq(mRequestJson));
        assertThat(code).isEqualTo(HttpURLConnection.HTTP_ACCEPTED);
    }

    @Test
    public void testDispatchResultWithErrorRequest() {
        final String url = newUriBuilder(RESULT_PATH)
                .appendQueryParameter(COMMAND_ID_KEY, COMMAND_ID)
                .appendQueryParameter(COMMAND_ERROR_KEY, ERROR)
                .build().toString();
        when(mRequest.getPath()).thenReturn(url);

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);

        verify(mResultsProvider).put(eq(COMMAND_ID), refEq(mRequestWithErrorJson));
        assertThat(code).isEqualTo(HttpURLConnection.HTTP_ACCEPTED);
    }

    @Test
    public void testDispatchCallbackRequest() {
        final String url = newUriBuilder(CALLBACK_PATH)
                .appendQueryParameter("listenerId", LISTENER_ID)
                .appendQueryParameter("callback", CALLBACK).build().toString();
        when(mRequest.getPath()).thenReturn(url);

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final String body = response.getBody().readUtf8();
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);

        verify(mCallbacksDispatcher).notify(eq(LISTENER_ID), eq(CALLBACK), refEq(mRequestJson), any());
        assertThat(body).isEqualTo(mCallbackResult.toString());
        assertThat(code).isEqualTo(HttpURLConnection.HTTP_ACCEPTED);
    }

    @Test
    public void testDispatchUnknownRequest() {
        final String url = newUriBuilder(UNKNOWN_PATH).build().toString();
        when(mRequest.getPath()).thenReturn(url);

        final MockResponse response = mCommandResponsePreparer.dispatch(mRequest);
        final int code = Integer.parseInt(response.getStatus().split(" ")[1]);

        assertThat(code).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    private Uri.Builder newUriBuilder(final String path) {
        return new Uri.Builder().scheme("http").authority("42.100.42.100")
                .path(path).appendPath("whatever")
                .appendQueryParameter("key", "value");
    }
}
