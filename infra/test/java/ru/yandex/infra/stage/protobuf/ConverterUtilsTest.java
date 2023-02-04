package ru.yandex.infra.stage.protobuf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class ConverterUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "15, 15",
            "2147483647, 2147483647",
            "-2147483648, 2147483648",
            "-1294967296, 3000000000",
            "-1, 4294967295"
    })
    public void testProtoUInt32ToLong(int protoUInt32, long expectedLong) {
        long actualLong = ConverterUtils.fromUInt32ToLong(protoUInt32);
        assertThatEquals(actualLong, expectedLong);
    }
}
