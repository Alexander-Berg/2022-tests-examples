package com.yandex.autotests;

import com.yandex.frankenstein.Log;
import com.yandex.frankenstein.annotations.TestCaseId;
import com.yandex.frankenstein.runner.FrankensteinJUnit4Runner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(FrankensteinJUnit4Runner.class)
public class ExampleTest {

    private static final String TAG = ExampleTest.class.getSimpleName();

    @TestCaseId(1)
    @Test
    public void testAdditionIsCorrect() {
        Log.e(TAG, "runner log");
        assertEquals(4, 2 + 2);
    }
}