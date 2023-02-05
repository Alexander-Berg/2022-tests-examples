package com.yandex.frankenstein;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommandProcessTest {

    private static final String COMMAND_ID = "commandId";
    private static final String VALUE_KEY = "value";
    private static final String STRING_VALUE = "string result";
    private static final int INT_VALUE = 42;
    private static final boolean BOOLEAN_VALUE = true;

    private final JSONObject mResult = new JSONObject();
    @Mock private Function<String, JSONObject> mResultsFunction;
    private CommandProcess mCommandProcess;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mResultsFunction.apply(COMMAND_ID)).thenReturn(mResult);

        mCommandProcess = new CommandProcess(COMMAND_ID, mResultsFunction);
    }

    @Test
    public void testGetCommandId() {
        assertThat(mCommandProcess.getCommandId()).isEqualTo(COMMAND_ID);
    }

    @Test
    public void testWaitFor() {
        mResult.put("key", "value");

        assertThat(mCommandProcess.waitFor())
                .usingComparator((json1, json2) -> json1.similar(json2) ? 0 : -1)
                .isEqualTo(mResult);
    }

    @Test(expected = RuntimeException.class)
    public void testWaitForThrowsExceptionIfResultHasStatusField() {
        mResult.put(CommandKeys.COMMAND_ERROR, "value");
        mCommandProcess.waitFor();
    }

    @Test
    public void testWaitForTwice() {
        mCommandProcess.waitFor();
        mCommandProcess.waitFor();

        verify(mResultsFunction, times(1)).apply(COMMAND_ID);
    }

    @Test
    public void testGetStringResult() {
        mResult.put(VALUE_KEY, STRING_VALUE);

        assertThat(mCommandProcess.getStringResult()).isEqualTo(STRING_VALUE);
    }

    @Test
    public void testGetIntResult() {
        mResult.put(VALUE_KEY, INT_VALUE);

        assertThat(mCommandProcess.getIntResult()).isEqualTo(INT_VALUE);
    }

    @Test
    public void testGetBooleanResult() {
        mResult.put(VALUE_KEY, BOOLEAN_VALUE);

        assertThat(mCommandProcess.getBooleanResult()).isEqualTo(BOOLEAN_VALUE);
    }

    @Test
    public void testGetResult() {
        mResult.put(VALUE_KEY, STRING_VALUE);

        assertThat(mCommandProcess.getResult()).isEqualTo(Collections.singletonMap(VALUE_KEY, STRING_VALUE));
    }

    @Test
    public void testGetResultWithNullValue() {
        mResult.put(VALUE_KEY, JSONObject.NULL);

        assertThat(mCommandProcess.getResult()).isEqualTo(Collections.singletonMap(VALUE_KEY, null));
    }
}
