package com.yandex.autotests;

import com.yandex.frankenstein.annotations.TestCaseId;
import com.yandex.frankenstein.runner.FrankensteinParametersRunnerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(FrankensteinParametersRunnerFactory.class)
public class ExampleParameterizedTest {

    @Parameterized.Parameter
    public int sum;

    @Parameterized.Parameter(1)
    public int x;

    @Parameterized.Parameter(2)
    public int y;

    @Parameterized.Parameters(name = "sum = {0}, x = {1}, y = {2}")
    public static List<Integer[]> parameters() {
        return Arrays.asList(new Integer[][]{{3, 1, 2}, {4, 2, 2}, {5, 2, 3}});
    }

    @TestCaseId(7)
    @Test
    public void testAdditionIsCorrect() {
        assertEquals(sum, x + y);
    }
}