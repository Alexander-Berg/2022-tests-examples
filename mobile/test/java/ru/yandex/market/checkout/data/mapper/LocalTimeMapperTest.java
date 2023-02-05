package ru.yandex.market.checkout.data.mapper;

import com.annimon.stream.Exceptional;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import ru.yandex.market.common.LocalTime;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.test.matchers.ExceptionalMatchers.containsError;

@RunWith(Enclosed.class)
public class LocalTimeMapperTest {

    @RunWith(Parameterized.class)
    public static class NormalMappingTests extends BaseTest {

        @Parameterized.Parameter
        public String input;

        @Parameterized.Parameter(1)
        public LocalTime expected;

        @Parameterized.Parameters(name = "{index}: map({0}) == {1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(
                    new Object[]{
                            "00:00:00",
                            LocalTime.builder()
                                    .hours(0)
                                    .minutes(0)
                                    .seconds(0)
                                    .build()
                    },
                    new Object[]{
                            "13:54:11",
                            LocalTime.builder()
                                    .hours(13)
                                    .minutes(54)
                                    .seconds(11)
                                    .build()
                    },
                    new Object[]{
                            "21:08",
                            LocalTime.builder()
                                    .hours(21)
                                    .minutes(8)
                                    .seconds(0)
                                    .build()
                    },
                    new Object[]{
                            "11",
                            LocalTime.builder()
                                    .hours(11)
                                    .minutes(0)
                                    .seconds(0)
                                    .build()
                    },
                    new Object[]{
                            "11:11:11:12",
                            LocalTime.builder()
                                    .hours(11)
                                    .minutes(11)
                                    .seconds(11)
                                    .build()
                    },
                    new Object[]{
                            "8:00:00",
                            LocalTime.builder()
                                    .hours(8)
                                    .minutes(0)
                                    .seconds(0)
                                    .build()
                    },
                    new Object[]{
                            "8",
                            LocalTime.builder()
                                    .hours(8)
                                    .minutes(0)
                                    .seconds(0)
                                    .build()
                    },
                    new Object[]{
                            "0008:00:00",
                            LocalTime.builder()
                                    .hours(8)
                                    .minutes(0)
                                    .seconds(0)
                                    .build()
                    }
            );
        }

        @Test
        public void testMappedLocalTimeSameAsExpected() throws Throwable {
            final LocalTime actual = mapper.map(input).getOrThrow();
            assertThat(actual, equalTo(expected));
        }
    }

    @RunWith(Parameterized.class)
    public static class ExceptionalMappingTests extends BaseTest {

        @Parameterized.Parameter
        public String input;

        @Parameterized.Parameters(name = "{index} -> {0}")
        public static Iterable<Object> data() {
            return Arrays.asList("24:00:00", "00:60:00", "00:00:60", null, "ab:cd:ef", "123:12:01");
        }

        @Test
        public void testReturnsErrorForIncorrectInput() {
            final Exceptional<LocalTime> exceptional = mapper.map(input);
            assertThat(exceptional, containsError());
        }
    }

    private static class BaseTest {

        protected LocalTimeMapper mapper;

        @Before
        public void setUp() {
            mapper = new LocalTimeMapper(new LocalTimeParser());
        }
    }
}
