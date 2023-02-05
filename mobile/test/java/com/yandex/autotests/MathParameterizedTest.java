package com.yandex.autotests;

import com.yandex.autotests.fakes.Math;
import com.yandex.autotests.runner.device.DeviceParametersRunnerFactory;
import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(DeviceParametersRunnerFactory.class)
public class MathParameterizedTest {

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

    @TestCaseId(5)
    @Test
    public void testAdditionIsCorrect() {
        assertEquals(sum, Math.add(x, y));
    }
}