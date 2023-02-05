package com.yandex.autotests;

import com.yandex.autotests.runner.filter.FilterParametersRunnerFactory;
import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(FilterParametersRunnerFactory.class)
public class FilterParameterizedTest {

    @Parameterized.Parameter
    public int x;

    @Parameterized.Parameters(name = "x = {0}")
    public static List<Integer> parameters() {
        return Arrays.asList(3, 2, 1);
    }

    @TestCaseId(8)
    @Test
    public void testAdditionIsCorrect() {
        assert x != 3;
    }
}