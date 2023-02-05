package com.yandex.frankenstein;

import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@SuppressWarnings("ConstantConditions")
public class CallbacksDispatcherTest {

    private static final String LISTENER_ID = "listener_id";
    private static final String UNKNOWN_LISTENER_ID = "unknown_listener_id";
    private static final String COMMAND = "command";
    private static final int CALLBACK_WAIT_TIMEOUT_SECONDS = 1;

    private final CallbacksDispatcher mCallbacksDispatcher = new CallbacksDispatcher(CALLBACK_WAIT_TIMEOUT_SECONDS);
    private final CallbacksDispatcher.Callback mCallback = mock(CallbacksDispatcher.Callback.class);

    private final JSONObject mArguments = new JSONObject().put("arguments_key", "arguments_value");
    private final JSONObject mResult = new JSONObject().put("result_key", "result_value");

    @Test
    public void testNotifyBeforePut() {
        new Thread(() -> {
            try {
                Thread.sleep(100);
                mCallbacksDispatcher.put(LISTENER_ID, mCallback);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        mCallbacksDispatcher.notify(LISTENER_ID, COMMAND, mArguments, mResult);

        verify(mCallback).notify(eq(COMMAND), refEq(mArguments), refEq(mResult), any(Executor.class));
    }

    @Test
    public void testNotifyAfterPut() {
        mCallbacksDispatcher.put(LISTENER_ID, mCallback);
        mCallbacksDispatcher.notify(LISTENER_ID, COMMAND, mArguments, mResult);

        verify(mCallback).notify(eq(COMMAND), refEq(mArguments), refEq(mResult), any(Executor.class));
    }

    @Test
    public void testNotifyWithUnknownListenerId() {
        mCallbacksDispatcher.put(LISTENER_ID, mCallback);
        mCallbacksDispatcher.notify(UNKNOWN_LISTENER_ID, COMMAND, mArguments, mResult);

        verifyZeroInteractions(mCallback);
    }

    @Test
    public void testNotifyWithoutPut() {
        mCallbacksDispatcher.notify(LISTENER_ID, COMMAND, mArguments, mResult);

        verifyZeroInteractions(mCallback);
    }
}
