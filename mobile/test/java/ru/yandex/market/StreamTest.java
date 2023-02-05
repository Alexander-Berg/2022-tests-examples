package ru.yandex.market;

import com.annimon.stream.Stream;

import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class StreamTest {

    @Test
    public void testAnyMatchOnEmptyStreamReturnsFalse() {
        assertThat(Stream.empty().anyMatch(o -> false), equalTo(false));
    }

    @Test
    public void testAnyMatchOnEmptyListStreamReturnsFalse() {
        assertThat(Stream.of(Collections.emptyList()).anyMatch(o -> false),
                equalTo(false));
    }
}
