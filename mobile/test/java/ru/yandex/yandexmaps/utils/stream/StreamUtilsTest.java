package ru.yandex.yandexmaps.utils.stream;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.yandexmaps.utils.stream.StreamUtils.permute;

public class StreamUtilsTest {

    @Test
    public void permuteNormally_whenEverythingIsOk() {
        assertStream(
                permute(asList("a", "b", "c"), asList("1", "2"), String::concat),
                "a1", "a2", "b1", "b2", "c1", "c2"
        );
    }

    @Test
    public void emptyStream_whenFirstIterableIsEmpty() {
        assertStream(permute(emptyList(), asList("1", "2"), String::concat));
    }

    @Test
    public void emptyStream_whenSecondIterableIsEmpty() {
        assertStream(permute(asList("1", "2"), emptyList(), String::concat));
    }

    @Test(expected = NullPointerException.class)
    public void throwNpe_whenFirstIterableContainsNull() {
        assertStream(permute(asList("a", null, "c"), singletonList("3"), String::concat));
    }

    @Test
    public void concat2Streams() {
        assertStream(StreamUtils.concat(Stream.of(1, 2), Stream.of(3, 4)), 1, 2, 3, 4);
    }

    @Test
    public void concat3Streams() {
        assertStream(StreamUtils.concat(Stream.of(1), Stream.of(2, 3), Stream.of(4, 5, 6)), 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void empty_whenEmptyStreams() {
        assertStream(StreamUtils.concat(Stream.empty(), Stream.empty(), Stream.empty(), Stream.empty()));
    }

    @Test
    public void tail_whenFirstEmpty(){
        assertStream(StreamUtils.concat(Stream.empty(), Stream.of(1, 2, 3)), 1, 2, 3);
    }

    @Test
    public void head_whenLastEmpty() {
        assertStream(StreamUtils.concat(Stream.of(1), Stream.empty(), Stream.empty()), 1);
    }

    @SafeVarargs
    final <T> void assertStream(Stream<? extends T> stream, final T... elements) {
        assertEquals(asList(elements), stream.collect(Collectors.toList()));
    }
}