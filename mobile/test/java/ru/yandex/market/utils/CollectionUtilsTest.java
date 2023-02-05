package ru.yandex.market.utils;

import androidx.annotation.NonNull;

import com.annimon.stream.Optional;
import com.annimon.stream.OptionalInt;
import com.annimon.stream.function.Predicate;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.annimon.stream.test.hamcrest.OptionalMatcher.hasValue;
import static com.annimon.stream.test.hamcrest.OptionalMatcher.isEmpty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class CollectionUtilsTest {

    public static class SimpleTests {

        @Test(expected = IllegalArgumentException.class)
        public void testConcatListsThrowsExceptionWhenFirstListIsNull() {
            //noinspection ConstantConditions
            CollectionUtils.concat(null, new ArrayList<>());
        }

        @Test(expected = IllegalArgumentException.class)
        public void testConcatListsThrowsExceptionWhenSecondListIsNull() {
            //noinspection ConstantConditions
            CollectionUtils.concat(new ArrayList<>(), null);
        }

        @Test
        public void testConcatEmptyListsResultIsAlsoEmpty() {
            final List<Object> result = CollectionUtils.concat(new ArrayList<>(), new ArrayList<>());
            assertThat(result, empty());
        }

        @Test
        public void testConcatNonEmptyListsResultContainsAllItemsInCorrectOrder() {
            final List<String> first = Arrays.asList("1", "2", "3");
            final List<String> second = Arrays.asList("4", "5", "6");
            final List<String> result = CollectionUtils.concat(first, second);
            assertThat(result, contains("1", "2", "3", "4", "5", "6"));
        }

        @Test
        public void testReturnsEmptyOptionalWhenIterableIsNull() {
            assertThat(CollectionUtils.getFirst((Iterable<?>) null), isEmpty());
        }

        @Test
        public void testReturnsEmptyOptionalWhenIterableIsEmpty() {
            assertThat(CollectionUtils.getFirst(Collections.emptyList()), isEmpty());
        }

        @Test
        public void testReturnsOptionalContainingFirstItemWhenIterableIsNotEmpty() {
            assertThat(CollectionUtils.getFirst(Arrays.asList("first", "second")),
                    hasValue("first"));
        }

        @Test
        public void testReturnsEmptyOptionalWhenArrayIsNull() {
            assertThat(CollectionUtils.getFirst((Object[]) null), isEmpty());
        }

        @Test
        public void testReturnsEmptyOptionalWhenArrayIsEmpty() {
            assertThat(CollectionUtils.getFirst(new Object[0]), isEmpty());
        }

        @Test
        public void testReturnsOptionalContainingFirstItemWhenArrayIsNotEmpty() {
            assertThat(CollectionUtils.getFirst(new String[]{"first", "second"}),
                    hasValue("first"));
        }

        @Test
        public void testReturnEmptyOptionalFromGetLastForNullIterable() {
            final Optional<String> lastElement = CollectionUtils.getLast(null);
            assertThat(lastElement, isEmpty());
        }

        @Test
        public void testReturnEmptyOptionalForEmptyIterable() {
            final Optional<String> lastElement = CollectionUtils.getLast(Collections.emptyList());
            assertThat(lastElement, isEmpty());
        }

        @Test
        public void testReturnLastElementForMultipleElementsIterable() {
            final Optional<String> lastElement = CollectionUtils.getLast(Arrays.asList("one", "two"));
            assertThat(lastElement, hasValue("two"));
        }

        @Test
        public void testReturnFirstElementFromOneElementIterable() {
            final Optional<String> lastElement = CollectionUtils.getLast(Collections.singleton("one"));
            assertThat(lastElement, hasValue("one"));
        }
    }

    @RunWith(Parameterized.class)
    public static class FindFirstPositionTest {

        @NonNull
        private final Iterable<String> iterable = Arrays.asList("0", "1", "2", "3", "4");

        private final int startPosition;
        @NonNull
        private final Predicate<String> predicate;
        @NonNull
        private final OptionalInt expectedResult;

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {0, wrap("3"::equals), OptionalInt.of(3)},
                    {0, wrap("5"::equals), OptionalInt.empty()},
                    {4, wrap("4"::equals), OptionalInt.of(4)},
                    {4, wrap("3"::equals), OptionalInt.empty()},
                    {5, wrap(it -> true), OptionalInt.empty()},
            });
        }

        @NonNull
        private static Predicate<String> wrap(@NonNull final Predicate<String> predicate) {
            return predicate;
        }

        public FindFirstPositionTest(
                final int startPosition,
                @NonNull final Predicate<String> predicate,
                @NonNull final OptionalInt expectedResult) {

            this.startPosition = startPosition;
            this.predicate = predicate;
            this.expectedResult = expectedResult;
        }

        @Test
        public void testFindFirstPosition() {
            final OptionalInt result = CollectionUtils.findFirstPosition(iterable, startPosition, predicate);
            assertThat(result, equalTo(expectedResult));
        }
    }
}