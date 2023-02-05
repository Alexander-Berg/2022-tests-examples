package com.yandex.frankenstein;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class ResultsProviderTest {

    private static final String TAG = ResultsProvider.class.getSimpleName();
    private static final String COMMAND_ID = "commandId";
    private static final String UNKNOWN_COMMAND_ID = "lastCommandId";
    private static final int RESULT_WAIT_TIMEOUT_SECONDS = 1;
    private static final JSONObject RESULT = new JSONObject().put("result_key", "result_value");

    private final ResultsProvider mResultsProvider = new ResultsProvider(RESULT_WAIT_TIMEOUT_SECONDS);

    @Parameterized.Parameter(value = 0)
    public String mTestName;

    @Parameterized.Parameter(value = 1)
    public BiFunction<ResultsProvider, String, JSONObject> mResultFunction;

    @Parameterized.Parameter(value = 2)
    public String mExpectedLog;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        final BiFunction<ResultsProvider, String, JSONObject> defaultFunction = ResultsProvider::get;
        final BiFunction<ResultsProvider, String, JSONObject> silentFunction = ResultsProvider::getSilently;
        return Arrays.asList(
                new Object[]{
                        "default",
                        defaultFunction,
                        String.format("result for command #%s: %s", COMMAND_ID, RESULT.toString(4))
                },
                new Object[]{
                        "silent",
                        silentFunction,
                        String.format("result for command #%s: %s", COMMAND_ID, "log disabled")
                }
        );
    }

    @Test
    public void testGetBeforePut() {
        putDelayed(COMMAND_ID, RESULT);
        final JSONObject actualResult = mResultFunction.apply(mResultsProvider, COMMAND_ID);

        assertSimilar(actualResult, RESULT);
    }

    @Test
    public void testGetAfterPut() {
        mResultsProvider.put(COMMAND_ID, RESULT);
        final JSONObject actualResult = mResultFunction.apply(mResultsProvider, COMMAND_ID);

        assertSimilar(actualResult, RESULT);
        verify(Logger.getLogger(TAG)).log(Level.INFO, mExpectedLog);
    }

    @Test(expected = RuntimeException.class)
    public void testGetWithUnknownCommandId() {
        mResultsProvider.put(COMMAND_ID, RESULT);
        mResultsProvider.get(UNKNOWN_COMMAND_ID);
    }

    @Test(expected = RuntimeException.class)
    public void testGetWithoutPut() {
        mResultsProvider.get(COMMAND_ID);
    }

    private void assertSimilar(@NotNull final JSONObject actual, @NotNull final JSONObject expected) {
        assertThat(actual)
                .usingComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .isEqualTo(expected);
    }

    private void putDelayed(@NotNull final String commandId, @NotNull final JSONObject result) {
        new Thread(() -> {
            try {
                Thread.sleep(100);
                mResultsProvider.put(commandId, result);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
